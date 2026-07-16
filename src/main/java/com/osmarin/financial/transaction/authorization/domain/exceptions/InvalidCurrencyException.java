package com.osmarin.financial.transaction.authorization.domain.exceptions;

public class InvalidCurrencyException extends DomainException {

    public InvalidCurrencyException(String message) {
        super(message);
    }

    public InvalidCurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
