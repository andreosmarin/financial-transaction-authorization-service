package com.osmarin.financial.transaction.authorization.application.commands;

import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import com.osmarin.financial.transaction.authorization.domain.models.Money;

import java.util.UUID;

public record AuthorizeTransactionCommand(
        UUID accountId,
        TransactionType type,
        Money amount
) {
}
