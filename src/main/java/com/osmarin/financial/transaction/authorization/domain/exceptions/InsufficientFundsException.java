package com.osmarin.financial.transaction.authorization.domain.exceptions;

import com.osmarin.financial.transaction.authorization.domain.models.Money;

public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException(Money requestedAmount, Money availableBalance) {
        super("Insufficient funds: requested " + requestedAmount + ", available " + availableBalance);
    }
}
