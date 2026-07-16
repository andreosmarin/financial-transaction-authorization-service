package com.osmarin.financial.transaction.authorization.application.services;

import com.osmarin.financial.transaction.authorization.application.commands.AuthorizeTransactionCommand;
import com.osmarin.financial.transaction.authorization.application.ports.input.AuthorizeTransactionUseCase;
import com.osmarin.financial.transaction.authorization.application.ports.output.AccountRepositoryPort;
import com.osmarin.financial.transaction.authorization.application.ports.output.TransactionIdGeneratorPort;
import com.osmarin.financial.transaction.authorization.application.ports.output.TransactionAuthorizationMetricsPort;
import com.osmarin.financial.transaction.authorization.application.ports.output.TransactionRepositoryPort;
import com.osmarin.financial.transaction.authorization.application.results.TransactionAuthorizationResult;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionStatus;
import com.osmarin.financial.transaction.authorization.domain.enums.TransactionType;
import com.osmarin.financial.transaction.authorization.domain.exceptions.AccountNotFoundException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InsufficientFundsException;
import com.osmarin.financial.transaction.authorization.domain.models.Account;
import com.osmarin.financial.transaction.authorization.domain.models.FinancialTransaction;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Objects;

@Service
public class AuthorizeTransactionService implements AuthorizeTransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeTransactionService.class);

    private final AccountRepositoryPort accountRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final TransactionIdGeneratorPort idGenerator;
    private final TransactionAuthorizationMetricsPort metrics;
    private final Clock clock;

    public AuthorizeTransactionService(AccountRepositoryPort accountRepository,
                                       TransactionRepositoryPort transactionRepository,
                                       TransactionIdGeneratorPort idGenerator,
                                       TransactionAuthorizationMetricsPort metrics,
                                       Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TransactionAuthorizationResult execute(AuthorizeTransactionCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        log.debug(
                "event=transaction_authorization_started accountId={} type={}",
                command.accountId(), command.type()
        );

        Account account = accountRepository.findByIdForUpdate(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        var timestamp = clock.instant();
        TransactionStatus status = authorize(account, command);
        if (status == TransactionStatus.SUCCEEDED) {
            accountRepository.save(account);
        }

        FinancialTransaction transaction = FinancialTransaction.completed(
                idGenerator.generate(), account.getId(), command.type(), command.amount(),
                status, timestamp
        );
        FinancialTransaction savedTransaction = transactionRepository.save(transaction);

        metrics.record(savedTransaction.getType(), savedTransaction.getStatus());
        logAuthorizationResult(savedTransaction);

        return new TransactionAuthorizationResult(savedTransaction, account);
    }

    private TransactionStatus authorize(Account account, AuthorizeTransactionCommand command) {
        Objects.requireNonNull(command.type(), "type must not be null");
        try {
            if (command.type() == TransactionType.CREDIT) {
                account.credit(command.amount());
            } else {
                account.debit(command.amount());
            }
            return TransactionStatus.SUCCEEDED;
        } catch (InsufficientFundsException exception) {
            log.debug(
                    "event=transaction_authorization_declined accountId={} type={} reason=INSUFFICIENT_FUNDS",
                    account.getId(), command.type()
            );
            return TransactionStatus.FAILED;
        }
    }

    private void logAuthorizationResult(FinancialTransaction transaction) {
        if (transaction.getStatus() == TransactionStatus.FAILED) {
            log.info(
                    "event=transaction_authorization_completed transactionId={} accountId={} type={} "
                            + "status={} reason=INSUFFICIENT_FUNDS",
                    transaction.getId(), transaction.getAccountId(), transaction.getType(),
                    transaction.getStatus()
            );
            return;
        }

        log.info(
                "event=transaction_authorization_completed transactionId={} accountId={} type={} status={}",
                transaction.getId(), transaction.getAccountId(), transaction.getType(),
                transaction.getStatus()
        );
    }
}
