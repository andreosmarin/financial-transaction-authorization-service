package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest;

import com.osmarin.financial.transaction.authorization.application.ports.input.AuthorizeTransactionUseCase;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest.dto.AuthorizeTransactionRequest;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest.dto.TransactionAuthorizationResponse;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionAuthorizationController implements TransactionAuthorizationApi {
    private final AuthorizeTransactionUseCase useCase;
    private final TransactionAuthorizationRestMapper mapper;

    public TransactionAuthorizationController(AuthorizeTransactionUseCase useCase,
                                              TransactionAuthorizationRestMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Override
    public TransactionAuthorizationResponse authorizeTransaction(AuthorizeTransactionRequest request) {
        return mapper.toResponse(useCase.execute(mapper.toCommand(request)));
    }
}
