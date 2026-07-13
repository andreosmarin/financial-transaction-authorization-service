package com.osmarin.financial.transaction.authorization.application.ports.output;

import com.osmarin.financial.transaction.authorization.domain.models.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryPort {
    boolean createIfAbsent(Account account);

    Optional<Account> findById(UUID id);

    Account save(Account account);

}