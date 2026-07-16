package com.osmarin.financial.transaction.authorization.application.results;

import com.osmarin.financial.transaction.authorization.domain.models.Account;
import com.osmarin.financial.transaction.authorization.domain.models.FinancialTransaction;

public record TransactionAuthorizationResult(
        FinancialTransaction transaction,
        Account account
) {
}
