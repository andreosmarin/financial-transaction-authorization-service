package com.osmarin.financial.transaction.authorization.domain.exceptions;

public class CurrencyMismatchException extends DomainException {
    public CurrencyMismatchException(String transactionCurrency, String accountCurrency) {
        super("Transaction currency " + transactionCurrency + " does not match account currency " + accountCurrency);
    }
}
