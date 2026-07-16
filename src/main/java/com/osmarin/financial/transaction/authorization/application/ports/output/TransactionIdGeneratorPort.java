package com.osmarin.financial.transaction.authorization.application.ports.output;

import java.util.UUID;

public interface TransactionIdGeneratorPort {
    UUID generate();
}
