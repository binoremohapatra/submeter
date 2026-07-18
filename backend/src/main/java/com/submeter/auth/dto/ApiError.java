package com.submeter.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Standard API error envelope.
 * <pre>
 * {
 *   "error": "INVALID_CREDENTIALS",
 *   "message": "Email or password is incorrect.",
 *   "status": 401,
 *   "retryAfterSeconds": null
 * }
 * </pre>
 * All error responses use this shape — never leak stack traces.
 */
public class ApiError {
    private final String error;
    private final String message;
    private final int status;
    /** Set for 429 responses. Null otherwise. */
    private final Long retryAfterSeconds;

    public ApiError(String error, String message, int status, Long retryAfterSeconds) {
        this.error = error;
        this.message = message;
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    // Getters
    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    // Builder
    public static ApiErrorBuilder builder() {
        return new ApiErrorBuilder();
    }

    public static class ApiErrorBuilder {
        private String error;
        private String message;
        private int status;
        private Long retryAfterSeconds;

        public ApiErrorBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ApiErrorBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ApiErrorBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ApiErrorBuilder retryAfterSeconds(Long retryAfterSeconds) {
            this.retryAfterSeconds = retryAfterSeconds;
            return this;
        }

        public ApiError build() {
            return new ApiError(error, message, status, retryAfterSeconds);
        }
    }
}
