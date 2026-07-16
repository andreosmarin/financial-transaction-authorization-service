package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.messaging.sqs;

import com.osmarin.financial.transaction.authorization.application.commands.CreateAccountCommand;
import com.osmarin.financial.transaction.authorization.application.ports.input.CreateAccountUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountCreatedConsumer.class);
    private final CreateAccountUseCase createAccountUseCase;
    private final AccountCreatedMessageMapper mapper;

    public AccountCreatedConsumer(
            CreateAccountUseCase createAccountUseCase,
            AccountCreatedMessageMapper mapper
    ) {
        this.createAccountUseCase = createAccountUseCase;
        this.mapper = mapper;
    }

    @SqsListener(
            "${application.messaging.sqs.account-created-queue}"
    )
    public void consume(AccountCreatedMessage message) {
        CreateAccountCommand command = mapper.toCommand(message);

        log.debug("event=account_created_message_received accountId={}", command.id());

        createAccountUseCase.execute(command);

        log.debug("event=account_created_message_processed accountId={}", command.id());
    }
}
