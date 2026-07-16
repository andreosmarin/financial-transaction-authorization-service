package com.osmarin.financial.transaction.authorization.domain.models;

import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidAmountException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class FinancialTransaction {

    private final UUID id;
    private final UUID accountId;
    private final TransactionType type;
    private final Money amount;
    private final TransactionStatus status;
    private final Instant timestamp;

    private FinancialTransaction(UUID id, UUID accountId, TransactionType type, Money amount,
                                 TransactionStatus status, Instant timestamp) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.amount = requireValidAmount(amount);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static FinancialTransaction completed(UUID id, UUID accountId, TransactionType type,
                                                  Money amount,
                                                  TransactionStatus status, Instant timestamp) {
        return new FinancialTransaction(id, accountId, type, amount, status, timestamp);
    }

    private static Money requireValidAmount(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        return amount;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public TransactionType getType() { return type; }
    public Money getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
}
