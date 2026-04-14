package com.dishes.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String DEFAULT_ERROR_MESSAGE = "request failed";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Envelope<Object>> handleBusiness(BusinessException ex) {
        var message = (ex.getMessage() == null || ex.getMessage().isBlank()) ? DEFAULT_ERROR_MESSAGE : ex.getMessage();
        log.warn("Business exception, code={}, message={}", ex.code().value(), message);
        return ResponseEntity.ok(Envelope.error(message, ex.code().value()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Envelope<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        var message = (ex.getMessage() == null || ex.getMessage().isBlank()) ? DEFAULT_ERROR_MESSAGE : ex.getMessage();
        return ResponseEntity.ok(Envelope.error(message));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Envelope<Object>> handleUnhandled(Throwable ex) {
        log.error("Unhandled API exception", ex);
        return ResponseEntity.ok(Envelope.error(DEFAULT_ERROR_MESSAGE));
    }
}
