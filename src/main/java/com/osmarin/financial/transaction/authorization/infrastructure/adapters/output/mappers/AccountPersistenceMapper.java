package com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.mappers;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import com.osmarin.financial.transaction.authorization.domain.models.Money;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceMapper {

    public AccountEntity toEntity(Account account) {
        AccountEntity entity = new AccountEntity();

        entity.setId(account.getId());
        entity.setOwnerId(account.getOwnerId());
        entity.setBalance(account.getBalance().amount());
        entity.setCurrency(account.getBalance().currency());
        entity.setStatus(account.getStatus());
        entity.setOpenedAt(account.getOpenedAt());

        return entity;
    }

    public Account toDomain(AccountEntity entity) {
        return Account.restore(
                entity.getId(),
                entity.getOwnerId(),
                Money.of(entity.getBalance(), entity.getCurrency()),
                AccountStatus.valueOf(entity.getStatus().name()),
                entity.getOpenedAt()
        );
    }
}
