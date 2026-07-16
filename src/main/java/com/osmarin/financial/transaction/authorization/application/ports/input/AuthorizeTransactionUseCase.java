package com.osmarin.financial.transaction.authorization.application.ports.input;

import com.osmarin.financial.transaction.authorization.application.commands.AuthorizeTransactionCommand;
import com.osmarin.financial.transaction.authorization.application.results.TransactionAuthorizationResult;

public interface AuthorizeTransactionUseCase {
    TransactionAuthorizationResult execute(AuthorizeTransactionCommand command);
}
