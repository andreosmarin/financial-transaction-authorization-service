package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.mappers;

import com.osmarin.financial.transaction.authorization.domain.models.FinancialTransaction;
import com.osmarin.financial.transaction.authorization.domain.models.Money;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionPersistenceMapper {
    public TransactionEntity toEntity(FinancialTransaction transaction) {
        var entity = new TransactionEntity();
        entity.setId(transaction.getId());
        entity.setAccountId(transaction.getAccountId());
        entity.setType(transaction.getType());
        entity.setAmount(transaction.getAmount().amount());
        entity.setCurrency(transaction.getAmount().currency());
        entity.setStatus(transaction.getStatus());
        entity.setOccurredAt(transaction.getTimestamp());
        return entity;
    }

    public FinancialTransaction toDomain(TransactionEntity entity) {
        return FinancialTransaction.completed(
                entity.getId(), entity.getAccountId(), entity.getType(),
                Money.of(entity.getAmount(), entity.getCurrency()),
                entity.getStatus(), entity.getOccurredAt()
        );
    }
}
