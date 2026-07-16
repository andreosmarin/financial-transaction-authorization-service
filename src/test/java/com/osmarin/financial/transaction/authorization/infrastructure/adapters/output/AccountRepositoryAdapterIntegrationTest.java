package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InsufficientFundsException;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import com.osmarin.financial.transaction.authorization.domain.models.FinancialTransaction;
import com.osmarin.financial.transaction.authorization.domain.models.Money;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.mappers.AccountPersistenceMapper;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.mappers.TransactionPersistenceMapper;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.AccountJpaRepository;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.TransactionJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import({AccountRepositoryAdapter.class, AccountPersistenceMapper.class,
        TransactionRepositoryAdapter.class, TransactionPersistenceMapper.class})
class AccountRepositoryAdapterIntegrationTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine")
    );

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }

    @Autowired
    private AccountRepositoryAdapter repository;

    @Autowired
    private AccountJpaRepository jpaRepository;

    @Autowired
    private TransactionRepositoryAdapter transactionRepository;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldInsertOnlyOnceAndRestorePersistedAccount() {
        UUID accountId = UUID.fromString("a74096e2-d897-4c7d-92c2-59092b79c943");
        Account account = Account.open(
                accountId,
                UUID.fromString("31fb61f8-dde5-456a-9062-5b92af091bd7"),
                AccountStatus.ENABLED,
                Instant.parse("2026-07-13T12:00:00Z")
        );

        boolean firstInsert = repository.createIfAbsent(account);
        boolean duplicateInsert = repository.createIfAbsent(account);

        assertThat(firstInsert).isTrue();
        assertThat(duplicateInsert).isFalse();
        assertThat(jpaRepository.count()).isOne();
        assertThat(repository.findById(accountId)).hasValueSatisfying(saved -> {
            assertThat(saved.getId()).isEqualTo(account.getId());
            assertThat(saved.getOwnerId()).isEqualTo(account.getOwnerId());
            assertThat(saved.getBalance().amount()).isEqualByComparingTo(new BigDecimal("0.00"));
            assertThat(saved.getBalance().currency()).isEqualTo("BRL");
            assertThat(saved.getStatus()).isEqualTo(AccountStatus.ENABLED);
            assertThat(saved.getOpenedAt()).isEqualTo(account.getOpenedAt());
        });
    }

    @Test
    void shouldUpdateLockedAccountAndPersistAuthorizationResult() {
        UUID accountId = UUID.fromString("5b19c8b6-0cc4-4c72-a989-0c2ee15fa975");
        Account account = Account.open(
                accountId, UUID.fromString("31fb61f8-dde5-456a-9062-5b92af091bd7"),
                AccountStatus.ENABLED, Instant.parse("2025-01-01T00:00:00Z")
        );
        repository.createIfAbsent(account);
        Instant updatedAtBeforeAuthorization = jpaRepository.findById(accountId)
                .orElseThrow()
                .getUpdatedAt();

        Account lockedAccount = repository.findByIdForUpdate(accountId).orElseThrow();
        lockedAccount.credit(Money.of(new BigDecimal("97.07"), "BRL"));
        repository.save(lockedAccount);
        FinancialTransaction transaction = transactionRepository.save(FinancialTransaction.completed(
                UUID.fromString("8e8ae808-b154-48b5-9f3e-553935cc4543"), accountId,
                TransactionType.CREDIT, Money.of(new BigDecimal("97.07"), "BRL"),
                TransactionStatus.SUCCEEDED, Instant.parse("2025-07-08T18:57:55Z")
        ));
        jpaRepository.flush();
        entityManager.clear();

        var updatedAccount = jpaRepository.findById(accountId).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("97.07");
        assertThat(updatedAccount.getUpdatedAt()).isAfter(updatedAtBeforeAuthorization);
        assertThat(transactionJpaRepository.findById(transaction.getId())).hasValueSatisfying(saved -> {
            assertThat(saved.getAccountId()).isEqualTo(accountId);
            assertThat(saved.getType()).isEqualTo(TransactionType.CREDIT);
            assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
            assertThat(saved.getAmount()).isEqualByComparingTo("97.07");
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSerializeConcurrentDebitsAndNeverOverdrawAccount() throws Exception {
        UUID accountId = UUID.fromString("c0f6c952-f4bc-4a30-956d-a26a27be34b5");
        var transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            Account account = Account.open(
                    accountId, UUID.fromString("31fb61f8-dde5-456a-9062-5b92af091bd7"),
                    AccountStatus.ENABLED, Instant.parse("2025-01-01T00:00:00Z")
            );
            account.credit(Money.of(new BigDecimal("100.00"), "BRL"));
            repository.createIfAbsent(account);
        });

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var debit = (java.util.concurrent.Callable<Boolean>) () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return transactionTemplate.execute(status -> {
                    Account account = repository.findByIdForUpdate(accountId).orElseThrow();
                    try {
                        account.debit(Money.of(new BigDecimal("80.00"), "BRL"));
                        repository.save(account);
                        return true;
                    } catch (InsufficientFundsException exception) {
                        return false;
                    }
                });
            };

            var first = executor.submit(debit);
            var second = executor.submit(debit);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(java.util.List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            )).containsExactlyInAnyOrder(true, false);
        }

        assertThat(jpaRepository.findById(accountId)).hasValueSatisfying(account ->
                assertThat(account.getBalance()).isEqualByComparingTo("20.00")
        );
    }
}
