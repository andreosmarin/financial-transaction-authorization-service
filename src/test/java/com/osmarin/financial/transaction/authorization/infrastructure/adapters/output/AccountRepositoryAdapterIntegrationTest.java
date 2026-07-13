package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.mappers.AccountPersistenceMapper;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.AccountJpaRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import({AccountRepositoryAdapter.class, AccountPersistenceMapper.class})
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
            assertThat(saved.getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
            assertThat(saved.getCurrency()).isEqualTo("BRL");
            assertThat(saved.getStatus()).isEqualTo(AccountStatus.ENABLED);
            assertThat(saved.getOpenedAt()).isEqualTo(account.getOpenedAt());
        });
    }
}
