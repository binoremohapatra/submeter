package com.submeter.entity;

import com.submeter.entity.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A human user who belongs to exactly one organization (v1 constraint).
 *
 * <p>Security fields:
 * <ul>
 *   <li>{@code passwordHash} — Argon2id hash via Spring Security's
 *       {@code Argon2PasswordEncoder}. Never returned in API responses.</li>
 *   <li>{@code failedLoginCount} — NOT NULL DEFAULT 0; incremented on each
 *       failed login attempt. Reset to 0 on successful login.</li>
 *   <li>{@code lockedUntil} — set by the rate-limiter on the 5th failed
 *       attempt; login returns 429 until this timestamp passes.</li>
 * </ul>
 *
 * <p>The Java class is named {@code User} (not {@code AppUser}) because the
 * table name is explicit via {@code @Table(name = "users")}, avoiding any
 * conflict with the PostgreSQL reserved word {@code USER}.
 *
 * <p>Soft-delete: setting {@code deletedAt} converts the user to a ghost actor
 * in the audit log (actor_id still resolves to the deleted user row).
 */
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User extends SoftDeletableEntity {

    /** The org this user belongs to. Immutable after creation (v1). */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    /** Globally unique; used as the login identifier. */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * Argon2id hash. Never serialize to JSON.
     * FLAG: NOT NULL — a null hash would allow login without a password check.
     */
    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * RBAC role within the organization.
     * Always re-verified against the DB on privileged actions (never trusted from JWT claim alone).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    /**
     * Flag is set when the user clicks the verification link.
     * In v1, the link is logged to console only (no email service).
     * FLAG: NOT NULL DEFAULT false — a null would require null-safe checks throughout auth.
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /** Updated on each successful login. Used in the audit log. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Counts consecutive failed login attempts. Reset to 0 on success.
     * FLAG: NOT NULL DEFAULT 0 — a null would cause NPE in the rate-limiter.
     */
    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    /**
     * If non-null and in the future, login is rejected with 429.
     * Set by the rate-limiter after {@code app.rate-limit.login-max-attempts} failures.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(int failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    // Builder
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Organization organization;
        private String email;
        private String passwordHash;
        private Role role;
        private boolean emailVerified = false;
        private Instant lastLoginAt;
        private int failedLoginCount = 0;
        private Instant lockedUntil;

        public UserBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserBuilder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public UserBuilder lastLoginAt(Instant lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
            return this;
        }

        public UserBuilder failedLoginCount(int failedLoginCount) {
            this.failedLoginCount = failedLoginCount;
            return this;
        }

        public UserBuilder lockedUntil(Instant lockedUntil) {
            this.lockedUntil = lockedUntil;
            return this;
        }

        public User build() {
            User user = new User();
            user.setOrganization(organization);
            user.setEmail(email);
            user.setPasswordHash(passwordHash);
            user.setRole(role);
            user.setEmailVerified(emailVerified);
            user.setLastLoginAt(lastLoginAt);
            user.setFailedLoginCount(failedLoginCount);
            user.setLockedUntil(lockedUntil);
            return user;
        }
    }
}
