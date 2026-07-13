package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output;

import com.osmarin.financial.transaction.authorization.application.ports.output.AccountRepositoryPort;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.mappers.AccountPersistenceMapper;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.AccountEntity;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.AccountJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository jpaRepository;
    private final AccountPersistenceMapper mapper;

    public AccountRepositoryAdapter(AccountJpaRepository jpaRepository, AccountPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public boolean createIfAbsent(Account account) {
        AccountEntity entity = mapper.toEntity(account);

        int affectedRows = jpaRepository.insertIfAbsent(
                entity.getId(),
                entity.getOwnerId(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.getStatus().name(),
                entity.getOpenedAt()
        );

        return affectedRows == 1;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Account save(Account account) {
        var saved = jpaRepository.save(mapper.toEntity(account));
        return mapper.toDomain(saved);
    }
}