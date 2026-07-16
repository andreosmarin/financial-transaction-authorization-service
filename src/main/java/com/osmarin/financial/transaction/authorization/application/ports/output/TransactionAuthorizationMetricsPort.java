package com.osmarin.financial.transaction.authorization.application.ports.output;

import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;

public interface TransactionAuthorizationMetricsPort {
    void record(TransactionType type, TransactionStatus status);
}
