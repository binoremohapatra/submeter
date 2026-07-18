package com.submeter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An end-customer billed by the organization.
 *
 * <p>Email uniqueness is per-org (two different orgs may have customers with the same email).
 * This is enforced via the partial index {@code idx_customers_org_email} in V2, not a
 * global UNIQUE constraint. The application layer validates uniqueness within the org
 * before INSERT/UPDATE.
 *
 * <p>Soft-delete cascade (executed by CustomerService, each step audited):
 * <ol>
 *   <li>Set {@code deletedAt} on this Customer.</li>
 *   <li>Transition all ACTIVE/TRIAL subscriptions to CANCELED
 *       with {@code cancellation_reason = 'customer_deleted'}.</li>
 *   <li>VOID all DRAFT/OPEN invoices.</li>
 *   <li>Retain PAID invoices and all usage events (financial audit trail).</li>
 * </ol>
 *
 * <p>FLAG: {@code email} is NOT NULL — required for mock invoice delivery logging
 * and for future real email delivery without a schema migration.
 */
@Entity
@Table(name = "customers")
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Contact email. NOT NULL.
     * Uniqueness within org enforced via V2 partial index + app-layer check.
     */
    @Column(name = "email", nullable = false)
    private String email;

    /** Optional phone number; no format validation in v1. */
    @Column(name = "phone")
    private String phone;

    /**
     * Freeform key-value store for caller-defined attributes.
     * Stored as PostgreSQL {@code jsonb}; deserialized to {@code Map<String, Object>}.
     * Default is an empty object — never null in Java or the DB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "is_senior", nullable = false)
    private boolean senior = false;

    @Column(name = "has_partner", nullable = false)
    private boolean partner = false;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Subscription> subscriptions = new ArrayList<>();

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isSenior() {
        return senior;
    }

    public void setSenior(boolean senior) {
        this.senior = senior;
    }

    public boolean isPartner() {
        return partner;
    }

    public void setPartner(boolean partner) {
        this.partner = partner;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    // Builder
    public static CustomerBuilder builder() {
        return new CustomerBuilder();
    }

    public static class CustomerBuilder {
        private Organization organization;
        private String name;
        private String email;
        private String phone;
        private Map<String, Object> metadata = new HashMap<>();
        private String externalId;
        private String gender;
        private boolean senior = false;
        private boolean partner = false;
        private List<Subscription> subscriptions = new ArrayList<>();

        public CustomerBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public CustomerBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CustomerBuilder email(String email) {
            this.email = email;
            return this;
        }

        public CustomerBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public CustomerBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public CustomerBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public CustomerBuilder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public CustomerBuilder senior(boolean senior) {
            this.senior = senior;
            return this;
        }

        public CustomerBuilder partner(boolean partner) {
            this.partner = partner;
            return this;
        }

        public CustomerBuilder subscriptions(List<Subscription> subscriptions) {
            this.subscriptions = subscriptions;
            return this;
        }

        public Customer build() {
            Customer customer = new Customer();
            customer.setOrganization(organization);
            customer.setName(name);
            customer.setEmail(email);
            customer.setPhone(phone);
            customer.setMetadata(metadata);
            customer.setExternalId(externalId);
            customer.setGender(gender);
            customer.setSenior(senior);
            customer.setPartner(partner);
            customer.setSubscriptions(subscriptions);
            return customer;
        }
    }
}
