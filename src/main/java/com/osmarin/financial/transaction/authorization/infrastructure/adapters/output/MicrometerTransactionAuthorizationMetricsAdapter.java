package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output;

import com.osmarin.financial.transaction.authorization.application.ports.output.TransactionAuthorizationMetricsPort;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MicrometerTransactionAuthorizationMetricsAdapter
        implements TransactionAuthorizationMetricsPort {

    private static final String METRIC_NAME = "financial.transaction.authorizations";
    private final Map<TransactionType, Map<TransactionStatus, Counter>> counters;

    public MicrometerTransactionAuthorizationMetricsAdapter(MeterRegistry meterRegistry) {
        counters = new EnumMap<>(TransactionType.class);
        for (TransactionType type : TransactionType.values()) {
            var countersByStatus = new EnumMap<TransactionStatus, Counter>(TransactionStatus.class);
            for (TransactionStatus status : TransactionStatus.values()) {
                countersByStatus.put(status, Counter.builder(METRIC_NAME)
                        .description("Number of completed transaction authorization attempts")
                        .tag("type", type.name())
                        .tag("status", status.name())
                        .register(meterRegistry));
            }
            counters.put(type, countersByStatus);
        }
    }

    @Override
    public void record(TransactionType type, TransactionStatus status) {
        Counter counter = counters.get(type).get(status);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            counter.increment();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                counter.increment();
            }
        });
    }
}
