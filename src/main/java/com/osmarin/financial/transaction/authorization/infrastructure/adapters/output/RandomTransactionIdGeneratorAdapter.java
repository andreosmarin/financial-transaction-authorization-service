package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output;

import com.osmarin.financial.transaction.authorization.application.ports.output.TransactionIdGeneratorPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RandomTransactionIdGeneratorAdapter implements TransactionIdGeneratorPort {
    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
