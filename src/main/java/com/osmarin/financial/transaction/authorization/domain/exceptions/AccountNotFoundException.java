package com.osmarin.financial.transaction.authorization.domain.exceptions;

import java.util.UUID;

public class AccountNotFoundException extends DomainException {
    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
    }
}
