package com.osmarin.financial.transaction.authorization.domain.exceptions;

public class InvalidBalanceException extends DomainException {

    public InvalidBalanceException(String message) {
        super(message);
    }
}
