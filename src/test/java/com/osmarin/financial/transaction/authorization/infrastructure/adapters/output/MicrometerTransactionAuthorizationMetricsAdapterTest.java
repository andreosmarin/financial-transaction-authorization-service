package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output;

import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerTransactionAuthorizationMetricsAdapterTest {

    @Test
    void shouldRecordAuthorizationWithoutHighCardinalityIdentifiers() {
        var registry = new SimpleMeterRegistry();
        var adapter = new MicrometerTransactionAuthorizationMetricsAdapter(registry);

        adapter.record(TransactionType.DEBIT, TransactionStatus.FAILED);
        adapter.record(TransactionType.DEBIT, TransactionStatus.FAILED);

        var counter = registry.get("financial.transaction.authorizations")
                .tags("type", "DEBIT", "status", "FAILED")
                .counter();

        assertThat(counter.count()).isEqualTo(2);
        assertThat(counter.getId().getTags())
                .extracting(tag -> tag.getKey())
                .containsExactlyInAnyOrder("type", "status");
    }

    @Test
    void shouldRecordOnlyAfterTransactionCommit() {
        var registry = new SimpleMeterRegistry();
        var adapter = new MicrometerTransactionAuthorizationMetricsAdapter(registry);
        TransactionSynchronizationManager.initSynchronization();

        try {
            adapter.record(TransactionType.CREDIT, TransactionStatus.SUCCEEDED);
            var counter = registry.get("financial.transaction.authorizations")
                    .tags("type", "CREDIT", "status", "SUCCEEDED")
                    .counter();

            assertThat(counter.count()).isZero();
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCommit());
            assertThat(counter.count()).isOne();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
