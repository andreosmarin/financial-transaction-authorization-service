package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.messaging.sqs;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountCreatedMessageMapperTest {

    private final AccountCreatedMessageMapper mapper = new AccountCreatedMessageMapper();

    @Test
    void shouldMapExternalPayloadToCommand() {
        AccountCreatedMessage message = message(" enabled ", "1783944000");

        var command = mapper.toCommand(message);

        assertThat(command.id()).isEqualTo(UUID.fromString("a74096e2-d897-4c7d-92c2-59092b79c943"));
        assertThat(command.ownerId()).isEqualTo(UUID.fromString("31fb61f8-dde5-456a-9062-5b92af091bd7"));
        assertThat(command.status()).isEqualTo(AccountStatus.ENABLED);
        assertThat(command.openedAt()).isEqualTo(Instant.ofEpochSecond(1783944000));
    }

    @Test
    void shouldRejectMessageWithoutAccountPayload() {
        assertThatThrownBy(() -> mapper.toCommand(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account-created message must contain account");
        assertThatThrownBy(() -> mapper.toCommand(new AccountCreatedMessage(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account-created message must contain account");
    }

    @Test
    void shouldRejectBlankStatus() {
        assertThatThrownBy(() -> mapper.toCommand(message(" ", "1783944000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account status must not be blank");
    }

    @Test
    void shouldRejectInvalidCreatedAt() {
        assertThatThrownBy(() -> mapper.toCommand(message("ENABLED", "not-a-timestamp")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account created_at must be a Unix timestamp in seconds")
                .hasCauseInstanceOf(NumberFormatException.class);
    }

    private AccountCreatedMessage message(String status, String createdAt) {
        return new AccountCreatedMessage(new AccountCreatedMessage.AccountPayload(
                "a74096e2-d897-4c7d-92c2-59092b79c943",
                "31fb61f8-dde5-456a-9062-5b92af091bd7",
                createdAt,
                status
        ));
    }
}
