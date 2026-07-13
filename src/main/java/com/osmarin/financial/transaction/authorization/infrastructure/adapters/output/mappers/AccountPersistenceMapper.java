package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.mappers;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceMapper {

    public AccountEntity toEntity(Account account) {
        AccountEntity entity = new AccountEntity();

        entity.setId(account.getId());
        entity.setOwnerId(account.getOwnerId());
        entity.setBalance(account.getBalance());
        entity.setCurrency(account.getCurrency());
        entity.setStatus(account.getStatus());
        entity.setOpenedAt(account.getOpenedAt());

        return entity;
    }

    public Account toDomain(AccountEntity entity) {
        return new Account(
                entity.getId(),
                entity.getOwnerId(),
                entity.getBalance(),
                entity.getCurrency(),
                AccountStatus.valueOf(entity.getStatus().name()),
                entity.getOpenedAt()
        );
    }
}