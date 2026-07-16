package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output;

import com.osmarin.financial.transaction.authorization.application.ports.output.TransactionRepositoryPort;
import com.osmarin.financial.transaction.authorization.domain.models.FinancialTransaction;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.mappers.TransactionPersistenceMapper;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.TransactionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {
    private final TransactionJpaRepository repository;
    private final TransactionPersistenceMapper mapper;

    public TransactionRepositoryAdapter(TransactionJpaRepository repository, TransactionPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public FinancialTransaction save(FinancialTransaction transaction) {
        return mapper.toDomain(repository.save(mapper.toEntity(transaction)));
    }
}
