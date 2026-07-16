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
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getCurrency()).isEqualTo("BRL");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ENABLED);
        assertThat(account.getOpenedAt()).isEqualTo(OPENED_AT);
    }

    @Test
    void shouldCreditAndDebitValidAmounts() {
        Account account = enabledAccountWithBalance("100.00");

        account.credit(new BigDecimal("25.50"), OPENED_AT.plusSeconds(1));
        boolean debited = account.debit(new BigDecimal("40.25"), OPENED_AT.plusSeconds(2));

        assertThat(debited).isTrue();
        assertThat(account.getBalance()).isEqualByComparingTo("85.25");
    }

    @Test
    void shouldRefuseDebitWhenBalanceIsInsufficientWithoutChangingIt() {
        Account account = enabledAccountWithBalance("10.00");

        boolean debited = account.debit(new BigDecimal("10.01"), OPENED_AT);

        assertThat(debited).isFalse();
        assertThat(account.getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldAllowDebitForExactBalance() {
        Account account = enabledAccountWithBalance("10.00");

        boolean debited = account.debit(new BigDecimal("10.00"), OPENED_AT);

        assertThat(debited).isTrue();
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRejectTransactionsForDisabledAccount() {
        Account account = Account.restore(
                ACCOUNT_ID, OWNER_ID, new BigDecimal("10.00"), "BRL",
                AccountStatus.DISABLED, OPENED_AT
        );

        assertThatThrownBy(() -> account.credit(new BigDecimal("1.00"), OPENED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Account is not enabled");
        assertThat(account.getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldRejectNonPositiveOrOverPreciseAmounts() {
        Account account = enabledAccountWithBalance("10.00");

        assertThatThrownBy(() -> account.credit(BigDecimal.ZERO, OPENED_AT))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Amount must be greater than zero");
        assertThatThrownBy(() -> account.debit(new BigDecimal("1.001"), OPENED_AT))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Amount must have at most two decimal places");
    }

    @Test
    void shouldNormalizeCurrencyWhenRestoringAccount() {
        Account withDefaultCurrency = Account.restore(
                ACCOUNT_ID, OWNER_ID, BigDecimal.ZERO, " ", AccountStatus.ENABLED, OPENED_AT
        );
        Account withLowercaseCurrency = Account.restore(
                ACCOUNT_ID, OWNER_ID, BigDecimal.ZERO, "usd", AccountStatus.ENABLED, OPENED_AT
        );

        assertThat(withDefaultCurrency.getCurrency()).isEqualTo("BRL");
        assertThat(withLowercaseCurrency.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldRejectInvalidInitialBalance() {
        assertThatThrownBy(() -> enabledAccountWithBalance("-0.01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Balance must not be negative");
        assertThatThrownBy(() -> enabledAccountWithBalance("1.001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Balance must have at most two decimal places");
    }

    private Account enabledAccountWithBalance(String balance) {
        return Account.restore(
                ACCOUNT_ID,
                OWNER_ID,
                new BigDecimal(balance),
                "BRL",
                AccountStatus.ENABLED,
                OPENED_AT
        );
    }
}
