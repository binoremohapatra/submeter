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

@Entity
@Table(name = "invitations")
@NoArgsConstructor
@AllArgsConstructor
public class Invitation extends SoftDeletableEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, ACCEPTED, EXPIRED, REVOKED

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false, updatable = false)
    private User invitedBy;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(User invitedBy) {
        this.invitedBy = invitedBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    // Builder
    public static InvitationBuilder builder() {
        return new InvitationBuilder();
    }

    public static class InvitationBuilder {
        private Organization organization;
        private String email;
        private Role role;
        private String tokenHash;
        private String status = "PENDING";
        private User invitedBy;
        private Instant expiresAt;

        public InvitationBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public InvitationBuilder email(String email) {
            this.email = email;
            return this;
        }

        public InvitationBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public InvitationBuilder tokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public InvitationBuilder status(String status) {
            this.status = status;
            return this;
        }

        public InvitationBuilder invitedBy(User invitedBy) {
            this.invitedBy = invitedBy;
            return this;
        }

        public InvitationBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Invitation build() {
            Invitation invitation = new Invitation();
            invitation.setOrganization(organization);
            invitation.setEmail(email);
            invitation.setRole(role);
            invitation.setTokenHash(tokenHash);
            invitation.setStatus(status);
            invitation.setInvitedBy(invitedBy);
            invitation.setExpiresAt(expiresAt);
            return invitation;
        }
    }
}
