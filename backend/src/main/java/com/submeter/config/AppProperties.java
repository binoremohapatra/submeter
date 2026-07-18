package com.submeter.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for all {@code app.*} properties.
 * Bound at startup via Spring Boot's {@code @ConfigurationProperties}.
 * Validation annotations ensure fail-fast on missing required values.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    @NotNull @Valid
    private Jwt jwt = new Jwt();

    @NotNull @Valid
    private Razorpay razorpay = new Razorpay();

    @NotNull @Valid
    private Billing billing = new Billing();

    @NotNull @Valid
    private RateLimit rateLimit = new RateLimit();

    @NotBlank
    private String storagePath = "./storage";

    /** When true: /api/dev/* endpoints are active and request logging is verbose. */
    private boolean devMode = false;

    // Getters and Setters
    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Razorpay getRazorpay() {
        return razorpay;
    }

    public void setRazorpay(Razorpay razorpay) {
        this.razorpay = razorpay;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(Billing billing) {
        this.billing = billing;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    // ── Nested config classes ─────────────────────────────────────────────────

    public static class Jwt {
        /** Min 32 chars (256 bits for HS256). Validated in JwtService @PostConstruct. */
        @NotBlank
        private String secret;
        private long accessTokenExpiryMs  = 900_000L;       // 15 minutes
        private long refreshTokenExpiryMs = 604_800_000L;   // 7 days
        /**
         * Set false in dev (HTTP). True in prod (HTTPS only).
         * Maps to {@code app.jwt.secure-cookie} in yaml.
         */
        private boolean secureCookie = true;

        // Getters and Setters
        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpiryMs() {
            return accessTokenExpiryMs;
        }

        public void setAccessTokenExpiryMs(long accessTokenExpiryMs) {
            this.accessTokenExpiryMs = accessTokenExpiryMs;
        }

        public long getRefreshTokenExpiryMs() {
            return refreshTokenExpiryMs;
        }

        public void setRefreshTokenExpiryMs(long refreshTokenExpiryMs) {
            this.refreshTokenExpiryMs = refreshTokenExpiryMs;
        }

        public boolean isSecureCookie() {
            return secureCookie;
        }

        public void setSecureCookie(boolean secureCookie) {
            this.secureCookie = secureCookie;
        }
    }

    public static class Razorpay {
        @NotBlank
        private String keyId;
        @NotBlank
        private String keySecret;
        private String webhookSecret = "";

        // Getters and Setters
        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getKeySecret() {
            return keySecret;
        }

        public void setKeySecret(String keySecret) {
            this.keySecret = keySecret;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }

    public static class Billing {
        private String nightlyCron    = "0 0 2 * * *"; // 02:00 UTC
        private int    paymentDueDays = 7;
        private int    pastDueRetryDays = 3;

        // Getters and Setters
        public String getNightlyCron() {
            return nightlyCron;
        }

        public void setNightlyCron(String nightlyCron) {
            this.nightlyCron = nightlyCron;
        }

        public int getPaymentDueDays() {
            return paymentDueDays;
        }

        public void setPaymentDueDays(int paymentDueDays) {
            this.paymentDueDays = paymentDueDays;
        }

        public int getPastDueRetryDays() {
            return pastDueRetryDays;
        }

        public void setPastDueRetryDays(int pastDueRetryDays) {
            this.pastDueRetryDays = pastDueRetryDays;
        }
    }

    public static class RateLimit {
        private int loginMaxAttempts   = 5;
        private int loginWindowMinutes = 15;

        // Getters and Setters
        public int getLoginMaxAttempts() {
            return loginMaxAttempts;
        }

        public void setLoginMaxAttempts(int loginMaxAttempts) {
            this.loginMaxAttempts = loginMaxAttempts;
        }

        public int getLoginWindowMinutes() {
            return loginWindowMinutes;
        }

        public void setLoginWindowMinutes(int loginWindowMinutes) {
            this.loginWindowMinutes = loginWindowMinutes;
        }
    }
}
