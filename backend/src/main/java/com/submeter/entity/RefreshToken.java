package com.submeter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Opaque refresh token stored in the DB.
 *
 * <p>Design rationale (see docs/architecture.md):
 * JWT access tokens are short-lived (15 min) and cannot be revoked early.
 * Refresh tokens are opaque UUIDs stored in this table, which allows
 * explicit revocation on logout or suspicious activity.
 *
 * <p>Rotation on use: each call to /api/auth/refresh revokes the current
 * token and issues a new one. Re-using a revoked refresh token returns 401.
 */
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /** UUID4 string. Never re-used. DB UNIQUE constraint prevents replay attacks. */
    @Column(name = "token", nullable = false, unique = true, updatable = false)
    private String token;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /** Set on logout or refresh rotation. Non-null = invalidated. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isValid() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    // Builder
    public static RefreshTokenBuilder builder() {
        return new RefreshTokenBuilder();
    }

    public static class RefreshTokenBuilder {
        private User user;
        private String token;
        private Instant expiresAt;
        private Instant revokedAt;

        public RefreshTokenBuilder user(User user) {
            this.user = user;
            return this;
        }

        public RefreshTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        public RefreshTokenBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public RefreshTokenBuilder revokedAt(Instant revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public RefreshToken build() {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUser(user);
            refreshToken.setToken(token);
            refreshToken.setExpiresAt(expiresAt);
            refreshToken.setRevokedAt(revokedAt);
            return refreshToken;
        }
    }
}
