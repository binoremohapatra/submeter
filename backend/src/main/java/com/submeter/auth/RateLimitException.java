package com.submeter.auth;

/**
 * Thrown when the rate limiter blocks a login attempt.
 * Mapped to HTTP 429 by {@code GlobalExceptionHandler}.
 */
public class RateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitException(long retryAfterSeconds) {
        super("Too many login attempts. Try again in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
