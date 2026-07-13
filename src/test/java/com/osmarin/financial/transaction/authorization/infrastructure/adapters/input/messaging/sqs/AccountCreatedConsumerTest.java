package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.messaging.sqs;

import com.osmarin.financial.transaction.authorization.application.commands.CreateAccountCommand;
import com.osmarin.financial.transaction.authorization.application.ports.input.CreateAccountUseCase;
import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountCreatedConsumerTest {

    @Test
    void shouldMapAndDispatchReceivedMessage() {
        CreateAccountUseCase useCase = mock(CreateAccountUseCase.class);
        AccountCreatedMessageMapper mapper = mock(AccountCreatedMessageMapper.class);
        AccountCreatedConsumer consumer = new AccountCreatedConsumer(useCase, mapper);
        AccountCreatedMessage message = new AccountCreatedMessage(null);
        CreateAccountCommand command = new CreateAccountCommand(
                UUID.randomUUID(), UUID.randomUUID(), AccountStatus.ENABLED, Instant.now()
        );
        when(mapper.toCommand(message)).thenReturn(command);

        consumer.consume(message);

        verify(mapper).toCommand(message);
        verify(useCase).execute(command);
    }
}
