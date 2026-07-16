package com.osmarin.financial.transaction.authorization.domain.models;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidAmountException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Account {

    private static final String DEFAULT_CURRENCY = "BRL";

    private final UUID id;
    private final UUID ownerId;
    private final Instant openedAt;
    private Money balance;
    private AccountStatus status;

    private Account(
            UUID id,
            UUID ownerId,
            Money balance,
            AccountStatus status,
            Instant openedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.balance = requireValidBalance(balance);
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
                Money.zero(DEFAULT_CURRENCY),
                status,
                openedAt
        );
    }

    public static Account restore(
            UUID id,
            UUID ownerId,
            Money balance,
            AccountStatus status,
            Instant openedAt
    ) {
        return new Account(
                id,
                ownerId,
                balance,
                status,
                openedAt
        );
    }

    private static void requireValidAmount(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
    }

    private static Money requireValidBalance(Money balance) {
        Objects.requireNonNull(balance, "balance must not be null");

        if (balance.isNegative()) {
            throw new IllegalArgumentException("Balance must not be negative");
        }
        return balance;
    }

    public void credit(Money amount) {
        requireEnabled();
        requireValidAmount(amount);

        balance = balance.add(amount);
    }

    public boolean debit(Money amount) {
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

    public Money getBalance() {
        return balance;
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
