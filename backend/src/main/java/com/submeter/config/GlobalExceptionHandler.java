package com.submeter.config;

import com.submeter.auth.AuthException;
import com.submeter.auth.RateLimitException;
import com.submeter.auth.dto.ApiError;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping.
 * Ensures every error response uses the {@link ApiError} envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuthException(AuthException ex) {
        HttpStatus status = "EMAIL_TAKEN".equals(ex.getErrorCode())
                ? HttpStatus.CONFLICT
                : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status)
                .body(ApiError.builder()
                        .error(ex.getErrorCode())
                        .message(ex.getMessage())
                        .status(status.value())
                        .build());
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(ApiError.builder()
                        .error("RATE_LIMITED")
                        .message(ex.getMessage())
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .retryAfterSeconds(ex.getRetryAfterSeconds())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult()
                .getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.builder()
                        .error("VALIDATION_FAILED")
                        .message(details)
                        .status(HttpStatus.BAD_REQUEST.value())
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.builder()
                        .error("VALIDATION_FAILED")
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.builder()
                        .error("INTERNAL_ERROR")
                        .message("An unexpected error occurred.")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build());
    }
}
