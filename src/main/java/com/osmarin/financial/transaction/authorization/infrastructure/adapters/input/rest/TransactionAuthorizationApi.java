package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest.dto.AuthorizeTransactionRequest;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest.dto.TransactionAuthorizationResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Transaction Authorization", description = "Financial Transaction Authorization API")
@RequestMapping("/api/transaction-authorization")
public interface TransactionAuthorizationApi {

    @Operation(summary = "Authorize a transaction")
    @ApiResponse(responseCode = "200", description = "Transaction processed; inspect status for approval or refusal")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @PostMapping(value = "/authorize", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    TransactionAuthorizationResponse authorizeTransaction(@Valid @RequestBody AuthorizeTransactionRequest request);

}
