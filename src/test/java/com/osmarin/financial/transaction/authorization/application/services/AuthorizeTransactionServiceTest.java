package com.osmarin.financial.transaction.authorization.application.services;

import com.osmarin.financial.transaction.authorization.application.commands.AuthorizeTransactionCommand;
import com.osmarin.financial.transaction.authorization.application.ports.output.AccountRepositoryPort;
import com.osmarin.financial.transaction.authorization.application.ports.output.TransactionIdGeneratorPort;
import com.osmarin.financial.transaction.authorization.application.ports.output.TransactionAuthorizationMetricsPort;
import com.osmarin.financial.transaction.authorization.application.ports.output.TransactionRepositoryPort;
import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import com.osmarin.financial.transaction.authorization.domain.exceptions.AccountNotFoundException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.CurrencyMismatchException;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import com.osmarin.financial.transaction.authorization.domain.models.FinancialTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizeTransactionServiceTest {
    private static final UUID ACCOUNT_ID = UUID.fromString("5b19c8b6-0cc4-4c72-a989-0c2ee15fa975");
    private static final UUID TRANSACTION_ID = UUID.fromString("8e8ae808-b154-48b5-9f3e-553935cc4543");
    private static final Instant NOW = Instant.parse("2025-07-08T18:57:55Z");

    @Mock AccountRepositoryPort accountRepository;
    @Mock TransactionRepositoryPort transactionRepository;
    @Mock TransactionIdGeneratorPort idGenerator;
    @Mock TransactionAuthorizationMetricsPort metrics;

    private AuthorizeTransactionService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizeTransactionService(
                accountRepository, transactionRepository, idGenerator, metrics,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldApproveCreditAndPersistAccountAndTransaction() {
        Account account = accountWithBalance("86.05");
        givenExistingAccount(account);

        var result = service.execute(command(TransactionType.CREDIT, "97.07", "brl"));

        assertThat(result.transaction().getId()).isEqualTo(TRANSACTION_ID);
        assertThat(result.transaction().getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
        assertThat(result.transaction().getTimestamp()).isEqualTo(NOW);
        assertThat(result.account().getBalance()).isEqualByComparingTo("183.12");
        verify(accountRepository).save(account);
        verify(transactionRepository).save(result.transaction());
        verify(metrics).record(TransactionType.CREDIT, TransactionStatus.SUCCEEDED);
    }

    @Test
    void shouldApproveDebitWhenFundsAreAvailable() {
        Account account = accountWithBalance("100.00");
        givenExistingAccount(account);

        var result = service.execute(command(TransactionType.DEBIT, "40.25", "BRL"));

        assertThat(result.transaction().getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
        assertThat(result.account().getBalance()).isEqualByComparingTo("59.75");
        verify(accountRepository).save(account);
    }

    @Test
    void shouldRefuseDebitWithoutFundsAndKeepBalanceWhileRecordingAttempt() {
        Account account = accountWithBalance("10.00");
        givenExistingAccount(account);

        var result = service.execute(command(TransactionType.DEBIT, "10.01", "BRL"));

        assertThat(result.transaction().getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(result.account().getBalance()).isEqualByComparingTo("10.00");
        verify(accountRepository, never()).save(account);
        verify(transactionRepository).save(result.transaction());
        verify(metrics).record(TransactionType.DEBIT, TransactionStatus.FAILED);
    }

    @Test
    void shouldRejectAuthorizationForUnknownAccount() {
        when(accountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(TransactionType.CREDIT, "1.00", "BRL")))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(ACCOUNT_ID.toString());
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectCurrencyDifferentFromAccountCurrency() {
        when(accountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(accountWithBalance("10.00")));

        assertThatThrownBy(() -> service.execute(command(TransactionType.CREDIT, "1.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessage("Transaction currency USD does not match account currency BRL");
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private void givenExistingAccount(Account account) {
        when(accountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(idGenerator.generate()).thenReturn(TRANSACTION_ID);
        when(transactionRepository.save(org.mockito.ArgumentMatchers.any(FinancialTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AuthorizeTransactionCommand command(TransactionType type, String amount, String currency) {
        return new AuthorizeTransactionCommand(ACCOUNT_ID, type, new BigDecimal(amount), currency);
    }

    private Account accountWithBalance(String balance) {
        return Account.restore(
                ACCOUNT_ID,
                UUID.fromString("31fb61f8-dde5-456a-9062-5b92af091bd7"),
                new BigDecimal(balance), "BRL", AccountStatus.ENABLED,
                Instant.parse("2025-01-01T00:00:00Z")
        );
    }
}
