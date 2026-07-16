package com.osmarin.financial.transaction.authorization.application.services;

import com.osmarin.financial.transaction.authorization.application.commands.CreateAccountCommand;
import com.osmarin.financial.transaction.authorization.application.ports.output.AccountRepositoryPort;
import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountServiceTest {

    @Mock
    private AccountRepositoryPort accountRepository;

    @InjectMocks
    private CreateAccountService service;

    @Test
    void shouldCreateAccountFromCommand() {
        CreateAccountCommand command = command();
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.createIfAbsent(accountCaptor.capture())).thenReturn(true);

        service.execute(command);

        verify(accountRepository).createIfAbsent(accountCaptor.getValue());
        Account account = accountCaptor.getValue();
        assertThat(account.getId()).isEqualTo(command.id());
        assertThat(account.getOwnerId()).isEqualTo(command.ownerId());
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ENABLED);
        assertThat(account.getOpenedAt()).isEqualTo(command.openedAt());
        assertThat(account.getBalance().amount()).isEqualByComparingTo("0.00");
        assertThat(account.getBalance().currency()).isEqualTo("BRL");
    }

    @Test
    void shouldTreatDuplicateAccountAsSuccessfulIdempotentProcessing() {
        CreateAccountCommand command = command();
        when(accountRepository.createIfAbsent(org.mockito.ArgumentMatchers.any(Account.class))).thenReturn(false);

        service.execute(command);

        verify(accountRepository).createIfAbsent(org.mockito.ArgumentMatchers.any(Account.class));
    }

    private CreateAccountCommand command() {
        return new CreateAccountCommand(
                UUID.fromString("a74096e2-d897-4c7d-92c2-59092b79c943"),
                UUID.fromString("31fb61f8-dde5-456a-9062-5b92af091bd7"),
                AccountStatus.ENABLED,
                Instant.parse("2026-07-13T12:00:00Z")
        );
    }
}
