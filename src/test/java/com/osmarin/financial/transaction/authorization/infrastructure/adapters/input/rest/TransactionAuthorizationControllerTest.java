package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest;

import com.osmarin.financial.transaction.authorization.application.ports.input.AuthorizeTransactionUseCase;
import com.osmarin.financial.transaction.authorization.application.results.TransactionAuthorizationResult;
import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import com.osmarin.financial.transaction.authorization.domain.exceptions.AccountNotEnabledException;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import com.osmarin.financial.transaction.authorization.domain.models.FinancialTransaction;
import com.osmarin.financial.transaction.authorization.domain.models.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionAuthorizationController.class)
@Import({TransactionAuthorizationRestMapper.class, ApiExceptionHandler.class})
class TransactionAuthorizationControllerTest {
    private static final UUID ACCOUNT_ID = UUID.fromString("5b19c8b6-0cc4-4c72-a989-0c2ee15fa975");

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthorizeTransactionUseCase useCase;

    @Test
    void shouldExposeAuthorizationContract() throws Exception {
        Account account = Account.restore(
                ACCOUNT_ID, UUID.randomUUID(), Money.of(new BigDecimal("183.12"), "BRL"),
                AccountStatus.ENABLED, Instant.parse("2025-01-01T00:00:00Z")
        );
        FinancialTransaction transaction = FinancialTransaction.completed(
                UUID.fromString("8e8ae808-b154-48b5-9f3e-553935cc4543"), ACCOUNT_ID,
                TransactionType.CREDIT, Money.of(new BigDecimal("97.07"), "BRL"),
                TransactionStatus.SUCCEEDED, Instant.parse("2025-07-08T18:57:55Z")
        );
        when(useCase.execute(any())).thenReturn(new TransactionAuthorizationResult(transaction, account));

        mockMvc.perform(post("/api/transaction-authorization/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","type":"CREDIT","amount":{"value":97.07,"currency":"BRL"}}
                                """.formatted(ACCOUNT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.id").value("8e8ae808-b154-48b5-9f3e-553935cc4543"))
                .andExpect(jsonPath("$.transaction.type").value("CREDIT"))
                .andExpect(jsonPath("$.transaction.amount.value").value(97.07))
                .andExpect(jsonPath("$.transaction.amount.currency").value("BRL"))
                .andExpect(jsonPath("$.transaction.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.transaction.timestamp").value("2025-07-08T18:57:55Z"))
                .andExpect(jsonPath("$.account.id").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.account.balance.amount").value(183.12))
                .andExpect(jsonPath("$.account.balance.currency").value("BRL"));
    }

    @Test
    void shouldRejectMalformedRequestBeforeUseCase() throws Exception {
        mockMvc.perform(post("/api/transaction-authorization/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","type":"DEBIT","amount":{"value":0,"currency":"BR"}}
                                """.formatted(ACCOUNT_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldTranslatePropagatedDomainError() throws Exception {
        when(useCase.execute(any())).thenThrow(new AccountNotEnabledException(ACCOUNT_ID));

        mockMvc.perform(post("/api/transaction-authorization/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","type":"DEBIT","amount":{"value":1.00,"currency":"BRL"}}
                                """.formatted(ACCOUNT_ID)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Transaction cannot be authorized"))
                .andExpect(jsonPath("$.detail").value("Account is not enabled: " + ACCOUNT_ID));
    }
}
