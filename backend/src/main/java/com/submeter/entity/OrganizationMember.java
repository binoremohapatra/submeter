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
@Table(name = "organization_members")
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMember extends SoftDeletableEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    // Builder
    public static OrganizationMemberBuilder builder() {
        return new OrganizationMemberBuilder();
    }

    public static class OrganizationMemberBuilder {
        private Organization organization;
        private User user;
        private Role role;
        private String status = "ACTIVE";
        private Instant joinedAt = Instant.now();

        public OrganizationMemberBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public OrganizationMemberBuilder user(User user) {
            this.user = user;
            return this;
        }

        public OrganizationMemberBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public OrganizationMemberBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OrganizationMemberBuilder joinedAt(Instant joinedAt) {
            this.joinedAt = joinedAt;
            return this;
        }

        public OrganizationMember build() {
            OrganizationMember member = new OrganizationMember();
            member.setOrganization(organization);
            member.setUser(user);
            member.setRole(role);
            member.setStatus(status);
            member.setJoinedAt(joinedAt);
            return member;
        }
    }
}
