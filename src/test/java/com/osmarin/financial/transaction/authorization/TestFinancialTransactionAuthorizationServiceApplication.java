package com.osmarin.financial.transaction.authorization;

import org.springframework.boot.SpringApplication;

public class TestFinancialTransactionAuthorizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(FinancialTransactionAuthorizationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
