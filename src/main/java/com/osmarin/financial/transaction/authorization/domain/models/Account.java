package com.osmarin.financial.transaction.authorization.domain.models;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidAmountException;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Account {

    private static final String DEFAULT_CURRENCY = "BRL";
    private static final int CURRENCY_SCALE = 2;

    private final UUID id;
    private final UUID ownerId;
    private final String currency;
    private final Instant openedAt;
    private BigDecimal balance;
    private AccountStatus status;

    public Account(
            UUID id,
            UUID ownerId,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Instant openedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.balance = requireValidBalance(balance);
        this.currency = requireValidCurrency(currency);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.openedAt = Objects.requireNonNull(openedAt, "createdAt must not be null");
    }

    public static Account open(
            UUID id,
            UUID ownerId,
            AccountStatus status,
            Instant openedAt
    ) {
        return new Account(
                id,
                ownerId,
                BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.UNNECESSARY),
                DEFAULT_CURRENCY,
                status,
                openedAt
        );
    }

    public static Account restore(
            UUID id,
            UUID ownerId,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Instant openedAt
    ) {
        return new Account(
                id,
                ownerId,
                balance,
                resolveCurrency(currency),
                status,
                openedAt
        );
    }

    private static void requireValidAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        if (amount.stripTrailingZeros().scale() > CURRENCY_SCALE) {
            throw new InvalidAmountException(
                    "Amount must have at most two decimal places"
            );
        }
    }

    private static BigDecimal requireValidBalance(BigDecimal balance) {
        Objects.requireNonNull(balance, "balance must not be null");

        if (balance.signum() < 0) {
            throw new IllegalArgumentException("Balance must not be negative");
        }

        if (balance.stripTrailingZeros().scale() > CURRENCY_SCALE) {
            throw new IllegalArgumentException(
                    "Balance must have at most two decimal places"
            );
        }

        return balance.setScale(CURRENCY_SCALE, RoundingMode.UNNECESSARY);
    }

    private static String resolveCurrency(String currency) {
        return StringUtils.isBlank(currency)
                ? DEFAULT_CURRENCY
                : requireValidCurrency(currency);
    }

    private static String requireValidCurrency(String currency) {
        Objects.requireNonNull(currency, "currency must not be null");

        var normalizedCurrency = currency.toUpperCase();

        if (normalizedCurrency.length() != 3) {
            throw new IllegalArgumentException(
                    "Only ISO4217 currency is supported"
            );
        }

        return normalizedCurrency;
    }

    public void credit(BigDecimal amount, Instant now) {
        requireEnabled();
        requireValidAmount(amount);

        balance = balance.add(amount);
    }

    public boolean debit(BigDecimal amount, Instant now) {
        requireEnabled();
        requireValidAmount(amount);

        if (balance.compareTo(amount) < 0) {
            return false;
        }

        balance = balance.subtract(amount);

        return true;
    }

    public boolean isEnabled() {
        return status == AccountStatus.ENABLED;
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException("Account is not enabled");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

}