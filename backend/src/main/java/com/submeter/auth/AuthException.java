package com.submeter.auth;

/**
 * Auth-layer business logic exception.
 * Thrown by {@link AuthService}, mapped to HTTP 401 by {@code GlobalExceptionHandler}.
 */
public class AuthException extends RuntimeException {

    private final String errorCode;

    public AuthException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
