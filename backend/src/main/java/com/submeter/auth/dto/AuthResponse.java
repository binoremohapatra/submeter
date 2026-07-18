package com.submeter.auth.dto;

import com.submeter.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * JSON response body returned on signup, login, and /me.
 * Token values are set as httpOnly cookies — never in this body.
 */
public class AuthResponse {
    private final UUID userId;
    private final UUID orgId;
    private final String email;
    private final Role role;
    private final String orgName;
    private final String orgSlug;

    public AuthResponse(UUID userId, UUID orgId, String email, Role role, String orgName, String orgSlug) {
        this.userId = userId;
        this.orgId = orgId;
        this.email = email;
        this.role = role;
        this.orgName = orgName;
        this.orgSlug = orgSlug;
    }

    // Getters
    public UUID getUserId() {
        return userId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public String getOrgName() {
        return orgName;
    }

    public String getOrgSlug() {
        return orgSlug;
    }

    // Builder
    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public static class AuthResponseBuilder {
        private UUID userId;
        private UUID orgId;
        private String email;
        private Role role;
        private String orgName;
        private String orgSlug;

        public AuthResponseBuilder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public AuthResponseBuilder orgId(UUID orgId) {
            this.orgId = orgId;
            return this;
        }

        public AuthResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public AuthResponseBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public AuthResponseBuilder orgName(String orgName) {
            this.orgName = orgName;
            return this;
        }

        public AuthResponseBuilder orgSlug(String orgSlug) {
            this.orgSlug = orgSlug;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(userId, orgId, email, role, orgName, orgSlug);
        }
    }
}
