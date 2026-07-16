package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.rest;

import com.osmarin.financial.transaction.authorization.domain.exceptions.AccountNotFoundException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.AccountNotEnabledException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.CurrencyMismatchException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.DomainException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidAmountException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidBalanceException;
import com.osmarin.financial.transaction.authorization.domain.exceptions.InvalidCurrencyException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleAccountNotFound(AccountNotFoundException exception, HttpServletRequest request) {
        logRejectedRequest(request, HttpStatus.NOT_FOUND, exception);
        return problem(HttpStatus.NOT_FOUND, "Account not found", exception.getMessage());
    }

    @ExceptionHandler({
            CurrencyMismatchException.class,
            InvalidAmountException.class,
            InvalidBalanceException.class,
            InvalidCurrencyException.class
    })
    ProblemDetail handleInvalidRequest(RuntimeException exception, HttpServletRequest request) {
        logRejectedRequest(request, HttpStatus.BAD_REQUEST, exception);
        return problem(HttpStatus.BAD_REQUEST, "Invalid transaction", exception.getMessage());
    }

    @ExceptionHandler(AccountNotEnabledException.class)
    ProblemDetail handleInvalidAccountState(AccountNotEnabledException exception, HttpServletRequest request) {
        logRejectedRequest(request, HttpStatus.UNPROCESSABLE_CONTENT, exception);
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Transaction cannot be authorized", exception.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomainFailure(DomainException exception, HttpServletRequest request) {
        logRejectedRequest(request, HttpStatus.UNPROCESSABLE_CONTENT, exception);
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Business rule violation", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationFailure(MethodArgumentNotValidException exception,
                                          HttpServletRequest request) {
        logRejectedRequest(request, HttpStatus.BAD_REQUEST, exception);
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request validation failed");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformedRequest(HttpMessageNotReadableException exception,
                                         HttpServletRequest request) {
        logRejectedRequest(request, HttpStatus.BAD_REQUEST, exception);
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Malformed JSON request");
    }

    private void logRejectedRequest(HttpServletRequest request, HttpStatus status, Exception exception) {
        log.warn(
                "event=api_request_rejected method={} path={} status={} errorType={}",
                request.getMethod(), request.getRequestURI(), status.value(),
                exception.getClass().getSimpleName()
        );
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
