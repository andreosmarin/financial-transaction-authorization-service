package com.osmarin.financial.transaction.authorization.domain.models;

import com.osmarin.financial.transaction.authorization.domain.exceptions.CurrencyMismatchException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidAmountException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidCurrencyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

public final class Money {

    public static final int SCALE = 2;
    private static final int MAX_PRECISION = 19;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.UNNECESSARY;

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = normalizeAmount(amount);
        this.currency = normalizeCurrency(currency);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void requireSameCurrency(Money other) {
        if (other == null) {
            throw new InvalidAmountException("Amount must not be null");
        }
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(other.currency, currency);
        }
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Amount must not be null");
        }

        try {
            BigDecimal normalized = amount.setScale(SCALE, ROUNDING_MODE);
            if (normalized.precision() > MAX_PRECISION) {
                throw new InvalidAmountException("Amount exceeds maximum supported precision");
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw new InvalidAmountException("Amount must have at most two decimal places");
        }
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null) {
            throw new InvalidCurrencyException("Currency must not be null");
        }
        String normalized = currency.strip().toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            throw new InvalidCurrencyException("Only ISO4217 currency is supported");
        }

        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCurrencyException("Only ISO4217 currency is supported", exception);
        }
        return normalized;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Money money)) return false;
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
