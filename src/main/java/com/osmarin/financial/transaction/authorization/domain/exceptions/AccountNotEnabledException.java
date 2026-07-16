package com.osmarin.financial.transaction.authorization.domain.exceptions;

import java.util.UUID;

public class AccountNotEnabledException extends DomainException {

    public AccountNotEnabledException(UUID accountId) {
        super("Account is not enabled: " + accountId);
    }
}
