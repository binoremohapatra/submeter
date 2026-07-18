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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "api_keys")
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey extends SoftDeletableEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(name = "key_id", nullable = false, unique = true, updatable = false)
    private String keyId;

    @Column(name = "prefix", nullable = false, updatable = false)
    private String prefix;

    @Column(name = "key_hash", nullable = false, unique = true, updatable = false)
    private String keyHash;

    @Column(name = "last_4", nullable = false, updatable = false)
    private String last4;

    @Column(name = "name", nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "scopes", columnDefinition = "text[]", nullable = false)
    private List<String> scopes = new ArrayList<>();

    @Column(name = "environment", nullable = false)
    private String environment = "PRODUCTION";

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getLast4() {
        return last4;
    }

    public void setLast4(String last4) {
        this.last4 = last4;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    // Builder
    public static ApiKeyBuilder builder() {
        return new ApiKeyBuilder();
    }

    public static class ApiKeyBuilder {
        private Organization organization;
        private String keyId;
        private String prefix;
        private String keyHash;
        private String last4;
        private String name;
        private List<String> scopes = new ArrayList<>();
        private String environment = "PRODUCTION";
        private User createdBy;
        private Instant lastUsedAt;
        private Instant revokedAt;

        public ApiKeyBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public ApiKeyBuilder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        public ApiKeyBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ApiKeyBuilder keyHash(String keyHash) {
            this.keyHash = keyHash;
            return this;
        }

        public ApiKeyBuilder last4(String last4) {
            this.last4 = last4;
            return this;
        }

        public ApiKeyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ApiKeyBuilder scopes(List<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public ApiKeyBuilder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public ApiKeyBuilder createdBy(User createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public ApiKeyBuilder lastUsedAt(Instant lastUsedAt) {
            this.lastUsedAt = lastUsedAt;
            return this;
        }

        public ApiKeyBuilder revokedAt(Instant revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public ApiKey build() {
            ApiKey apiKey = new ApiKey();
            apiKey.setOrganization(organization);
            apiKey.setKeyId(keyId);
            apiKey.setPrefix(prefix);
            apiKey.setKeyHash(keyHash);
            apiKey.setLast4(last4);
            apiKey.setName(name);
            apiKey.setScopes(scopes);
            apiKey.setEnvironment(environment);
            apiKey.setCreatedBy(createdBy);
            apiKey.setLastUsedAt(lastUsedAt);
            apiKey.setRevokedAt(revokedAt);
            return apiKey;
        }
    }
}
