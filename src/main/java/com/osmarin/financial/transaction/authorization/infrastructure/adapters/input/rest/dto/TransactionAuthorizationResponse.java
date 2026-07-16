package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest.dto;

import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionAuthorizationResponse(
        Transaction transaction,
        Account account
) {
    public record Transaction(
            UUID id,
            TransactionType type,
            TransactionAmount amount,
            TransactionStatus status,
            Instant timestamp
    ) {
    }

    public record TransactionAmount(BigDecimal value, String currency) {
    }

    public record Account(UUID id, Balance balance) {
    }

    public record Balance(BigDecimal amount, String currency) {
    }
}
