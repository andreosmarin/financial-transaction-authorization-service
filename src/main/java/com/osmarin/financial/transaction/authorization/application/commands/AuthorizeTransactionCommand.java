package com.osmarin.financial.transaction.authorization.application.commands;

import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record AuthorizeTransactionCommand(
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        String currency
) {
}
