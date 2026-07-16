package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest.dto;

import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record AuthorizeTransactionRequest(
        @NotNull UUID accountId,
        @NotNull TransactionType type,
        @NotNull @Valid Amount amount
) {
    public record Amount(
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal value,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency
    ) {
    }
}
