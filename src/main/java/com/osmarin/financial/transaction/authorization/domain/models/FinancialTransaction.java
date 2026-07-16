package com.osmarin.financial.transaction.authorization.domain.models;

import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class FinancialTransaction {

    private static final int CURRENCY_SCALE = 2;

    private final UUID id;
    private final UUID accountId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String currency;
    private final TransactionStatus status;
    private final Instant timestamp;

    public FinancialTransaction(UUID id, UUID accountId, TransactionType type, BigDecimal amount,
                                String currency, TransactionStatus status, Instant timestamp) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.amount = requireValidAmount(amount);
        this.currency = requireIsoCurrency(currency);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static FinancialTransaction completed(UUID id, UUID accountId, TransactionType type,
                                                  BigDecimal amount, String currency,
                                                  TransactionStatus status, Instant timestamp) {
        return new FinancialTransaction(id, accountId, type, amount, currency, status, timestamp);
    }

    private static BigDecimal requireValidAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > CURRENCY_SCALE) {
            throw new InvalidAmountException("Amount must have at most two decimal places");
        }
        return amount.setScale(CURRENCY_SCALE, RoundingMode.UNNECESSARY);
    }

    private static String requireIsoCurrency(String currency) {
        Objects.requireNonNull(currency, "currency must not be null");
        String normalized = currency.strip().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Only ISO4217 currency is supported", exception);
        }
        return normalized;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionStatus getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
}
