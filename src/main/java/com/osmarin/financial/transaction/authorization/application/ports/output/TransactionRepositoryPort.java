package com.osmarin.financial.transaction.authorization.application.ports.output;

import com.osmarin.financial.transaction.authorization.domain.models.FinancialTransaction;

public interface TransactionRepositoryPort {
    FinancialTransaction save(FinancialTransaction transaction);
}
