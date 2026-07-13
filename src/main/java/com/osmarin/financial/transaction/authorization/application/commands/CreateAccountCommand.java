package com.osmarin.financial.transaction.authorization.application.commands;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record CreateAccountCommand(
        UUID id,
        UUID ownerId,
        AccountStatus status,
        Instant openedAt
) {
}
