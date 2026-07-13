package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.messaging.sqs;

import com.osmarin.financial.transaction.authorization.application.commands.CreateAccountCommand;
import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Component
public class AccountCreatedMessageMapper {

    CreateAccountCommand toCommand(
            AccountCreatedMessage message
    ) {
        if (message == null || message.account() == null) {
            throw new IllegalArgumentException(
                    "Account-created message must contain account"
            );
        }

        var payload = message.account();

        return new CreateAccountCommand(
                UUID.fromString(payload.id()),
                UUID.fromString(payload.owner()),
                parseStatus(payload.status()),
                parseOpenedAt(payload.createdAt())
        );
    }

    private AccountStatus parseStatus(String status) {
        if (StringUtils.isBlank(status)) {
            throw new IllegalArgumentException(
                    "Account status must not be blank"
            );
        }

        return AccountStatus.valueOf(
                status.trim().toUpperCase(Locale.ROOT)
        );
    }

    private Instant parseOpenedAt(String createdAt) {
        if (StringUtils.isBlank(createdAt)) {
            throw new IllegalArgumentException(
                    "Account created_at must not be blank"
            );
        }

        try {
            return Instant.ofEpochSecond(
                    Long.parseLong(createdAt)
            );
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Account created_at must be a Unix timestamp in seconds",
                    exception
            );
        }
    }
}