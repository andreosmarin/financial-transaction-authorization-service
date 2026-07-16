package com.osmarin.financial.transaction.authorization.domain.models;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidAmountException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("a74096e2-d897-4c7d-92c2-59092b79c943");
    private static final UUID OWNER_ID = UUID.fromString("31fb61f8-dde5-456a-9062-5b92af091bd7");
    private static final Instant OPENED_AT = Instant.parse("2026-07-13T12:00:00Z");

    @Test
    void shouldOpenAnEnabledAccountWithDefaults() {
        Account account = Account.open(ACCOUNT_ID, OWNER_ID, AccountStatus.ENABLED, OPENED_AT);

        assertThat(account.getId()).isEqualTo(ACCOUNT_ID);
        assertThat(account.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(account.getBalance()).isEqualTo(Money.zero("BRL"));
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ENABLED);
        assertThat(account.getOpenedAt()).isEqualTo(OPENED_AT);
    }

    @Test
    void shouldCreditAndDebitValidAmounts() {
        Account account = enabledAccountWithBalance("100.00");

        account.credit(money("25.50"));
        boolean debited = account.debit(money("40.25"));

        assertThat(debited).isTrue();
        assertThat(account.getBalance()).isEqualTo(money("85.25"));
    }

    @Test
    void shouldRefuseDebitWhenBalanceIsInsufficientWithoutChangingIt() {
        Account account = enabledAccountWithBalance("10.00");

        boolean debited = account.debit(money("10.01"));

        assertThat(debited).isFalse();
        assertThat(account.getBalance()).isEqualTo(money("10.00"));
    }

    @Test
    void shouldAllowDebitForExactBalance() {
        Account account = enabledAccountWithBalance("10.00");

        boolean debited = account.debit(money("10.00"));

        assertThat(debited).isTrue();
        assertThat(account.getBalance()).isEqualTo(Money.zero("BRL"));
    }

    @Test
    void shouldRejectTransactionsForDisabledAccount() {
        Account account = Account.restore(
                ACCOUNT_ID, OWNER_ID, money("10.00"),
                AccountStatus.DISABLED, OPENED_AT
        );

        assertThatThrownBy(() -> account.credit(money("1.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Account is not enabled");
        assertThat(account.getBalance()).isEqualTo(money("10.00"));
    }

    @Test
    void shouldRejectNonPositiveOrOverPreciseAmounts() {
        Account account = enabledAccountWithBalance("10.00");

        assertThatThrownBy(() -> account.credit(Money.zero("BRL")))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Amount must be greater than zero");
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.001"), "BRL"))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Amount must have at most two decimal places");
    }

    @Test
    void shouldRejectOperationWithCurrencyDifferentFromAccount() {
        Account account = enabledAccountWithBalance("10.00");

        assertThatThrownBy(() -> account.credit(Money.of(new BigDecimal("1.00"), "USD")))
                .isInstanceOf(com.osmarin.financial.transaction.authorization.domain.exceptions.CurrencyMismatchException.class)
                .hasMessage("Transaction currency USD does not match account currency BRL");
        assertThat(account.getBalance()).isEqualTo(money("10.00"));
    }

    @Test
    void shouldRejectInvalidInitialBalance() {
        assertThatThrownBy(() -> enabledAccountWithBalance("-0.01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Balance must not be negative");
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.001"), "BRL"))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Amount must have at most two decimal places");
    }

    private Account enabledAccountWithBalance(String balance) {
        return Account.restore(
                ACCOUNT_ID,
                OWNER_ID,
                money(balance),
                AccountStatus.ENABLED,
                OPENED_AT
        );
    }

    private Money money(String amount) {
        return Money.of(new BigDecimal(amount), "BRL");
    }
}
