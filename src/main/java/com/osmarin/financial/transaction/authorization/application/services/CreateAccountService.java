package com.osmarin.financial.transaction.authorization.application.services;

import com.osmarin.financial.transaction.authorization.application.commands.CreateAccountCommand;
import com.osmarin.financial.transaction.authorization.application.ports.input.CreateAccountUseCase;
import com.osmarin.financial.transaction.authorization.application.ports.output.AccountRepositoryPort;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreateAccountService implements CreateAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateAccountService.class);
    private final AccountRepositoryPort accountRepository;

    public CreateAccountService(
            AccountRepositoryPort accountRepository
    ) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public void execute(CreateAccountCommand command) {
        var account = Account.open(
                command.id(),
                command.ownerId(),
                command.status(),
                command.openedAt()
        );

        boolean created = accountRepository.createIfAbsent(account);

        if (created) {
            log.info("Account created: accountId={}", account.getId());
            return;
        }

        log.debug("Duplicate account-created event ignored: accountId={}", account.getId());
    }
}
