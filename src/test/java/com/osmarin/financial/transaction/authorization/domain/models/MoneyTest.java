package com.osmarin.financial.transaction.authorization.domain.models;

import com.osmarin.financial.transaction.authorization.domain.exceptions.CurrencyMismatchException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidAmountException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidCurrencyException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void shouldNormalizeCurrencyAndScaleWithoutLosingPrecision() {
        Money money = Money.of(new BigDecimal("10.1"), " brl ");

        assertThat(money.amount()).isEqualTo(new BigDecimal("10.10"));
        assertThat(money.currency()).isEqualTo("BRL");
    }

    @Test
    void shouldPerformExactDecimalArithmetic() {
        Money result = Money.of(new BigDecimal("0.10"), "BRL")
                .add(Money.of(new BigDecimal("0.20"), "BRL"))
                .subtract(Money.of(new BigDecimal("0.05"), "BRL"));

        assertThat(result).isEqualTo(Money.of(new BigDecimal("0.25"), "BRL"));
    }

    @Test
    void shouldRejectSilentRounding() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.001"), "BRL"))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Amount must have at most two decimal places");
    }

    @Test
    void shouldRespectDatabasePrecisionBoundary() {
        assertThat(Money.of(new BigDecimal("99999999999999999.99"), "BRL").amount())
                .isEqualTo(new BigDecimal("99999999999999999.99"));

        assertThatThrownBy(() -> Money.of(new BigDecimal("100000000000000000.00"), "BRL"))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Amount exceeds maximum supported precision");
    }

    @Test
    void shouldRejectArithmeticBetweenDifferentCurrencies() {
        Money reais = Money.of(new BigDecimal("10.00"), "BRL");
        Money dollars = Money.of(new BigDecimal("1.00"), "USD");

        assertThatThrownBy(() -> reais.add(dollars))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessage("Transaction currency USD does not match account currency BRL");
    }

    @Test
    void shouldRejectUnknownIsoCurrency() {
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, "XYZ"))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessage("Only ISO4217 currency is supported");
    }
}
