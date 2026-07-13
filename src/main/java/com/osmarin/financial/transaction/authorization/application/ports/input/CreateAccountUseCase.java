package com.osmarin.financial.transaction.authorization.application.ports.input;

import com.osmarin.financial.transaction.authorization.application.commands.CreateAccountCommand;

public interface CreateAccountUseCase {
    void execute(CreateAccountCommand command);
}
