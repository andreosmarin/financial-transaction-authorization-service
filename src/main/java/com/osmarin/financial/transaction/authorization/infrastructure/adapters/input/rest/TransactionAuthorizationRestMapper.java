package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest;

import com.osmarin.financial.transaction.authorization.application.commands.AuthorizeTransactionCommand;
import com.osmarin.financial.transaction.authorization.application.results.TransactionAuthorizationResult;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest.dto.AuthorizeTransactionRequest;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest.dto.TransactionAuthorizationResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionAuthorizationRestMapper {
    public AuthorizeTransactionCommand toCommand(AuthorizeTransactionRequest request) {
        return new AuthorizeTransactionCommand(
                request.accountId(), request.type(), request.amount().value(), request.amount().currency()
        );
    }

    public TransactionAuthorizationResponse toResponse(TransactionAuthorizationResult result) {
        var transaction = result.transaction();
        var account = result.account();
        return new TransactionAuthorizationResponse(
                new TransactionAuthorizationResponse.Transaction(
                        transaction.getId(),
                        transaction.getType(),
                        new TransactionAuthorizationResponse.TransactionAmount(
                                transaction.getAmount(), transaction.getCurrency()
                        ),
                        transaction.getStatus(),
                        transaction.getTimestamp()
                ),
                new TransactionAuthorizationResponse.Account(
                        account.getId(),
                        new TransactionAuthorizationResponse.Balance(
                                account.getBalance(), account.getCurrency()
                        )
                )
        );
    }
}
