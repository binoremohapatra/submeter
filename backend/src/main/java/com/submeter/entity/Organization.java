package com.submeter.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Root tenant entity. Every other table is scoped to an org via {@code org_id}.
 *
 * <p>Multi-tenancy isolation contract (see docs/architecture.md):
 * Every query on a tenant-scoped table MUST include {@code WHERE org_id = :orgId}.
 * This is enforced at the repository/service layer — not via RLS (deferred, see arch doc).
 *
 * <p>Soft-delete: setting {@code deletedAt} on an org has cascading implications
 * that are handled at the service layer with explicit cascade logic, not JPA cascades,
 * to ensure each step is audited.
 */
@Entity
@Table(name = "organizations")
@NoArgsConstructor
@AllArgsConstructor
public class Organization extends SoftDeletableEntity {

    /** Human-readable org name shown in the UI. */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * URL-safe unique identifier (e.g. {@code "acme-corp"}).
     * Globally unique — used for future API key scoping and subdomain routing.
     * Set on creation; immutable thereafter.
     */
    @Column(name = "slug", nullable = false, unique = true, updatable = false)
    private String slug;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "timezone", nullable = false)
    private String timezone = "UTC";

    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    @Column(name = "support_email")
    private String supportEmail;

    @Column(name = "default_tax_rate")
    private Long defaultTaxRate;

    @Column(name = "invoice_prefix")
    private String invoicePrefix;

    @Column(name = "invoice_footer")
    private String invoiceFooter;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(name = "company_address")
    private String companyAddress;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    // ── Relationships ────────────────────────────────────────────────────────
    // LAZY across the board — never pull the whole graph on a list query.

    @JsonIgnore
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private List<Customer> customers = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private List<Plan> plans = new ArrayList<>();

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public Long getDefaultTaxRate() {
        return defaultTaxRate;
    }

    public void setDefaultTaxRate(Long defaultTaxRate) {
        this.defaultTaxRate = defaultTaxRate;
    }

    public String getInvoicePrefix() {
        return invoicePrefix;
    }

    public void setInvoicePrefix(String invoicePrefix) {
        this.invoicePrefix = invoicePrefix;
    }

    public String getInvoiceFooter() {
        return invoiceFooter;
    }

    public void setInvoiceFooter(String invoiceFooter) {
        this.invoiceFooter = invoiceFooter;
    }

    public String getCompanyWebsite() {
        return companyWebsite;
    }

    public void setCompanyWebsite(String companyWebsite) {
        this.companyWebsite = companyWebsite;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    public void setPlans(List<Plan> plans) {
        this.plans = plans;
    }

    // Builder
    public static OrganizationBuilder builder() {
        return new OrganizationBuilder();
    }

    public static class OrganizationBuilder {
        private String name;
        private String slug;
        private String logoUrl;
        private String timezone = "UTC";
        private String currency = "INR";
        private String supportEmail;
        private Long defaultTaxRate;
        private String invoicePrefix;
        private String invoiceFooter;
        private String companyWebsite;
        private String companyAddress;
        private Long version = 0L;
        private List<User> users = new ArrayList<>();
        private List<Customer> customers = new ArrayList<>();
        private List<Plan> plans = new ArrayList<>();

        public OrganizationBuilder name(String name) {
            this.name = name;
            return this;
        }

        public OrganizationBuilder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public OrganizationBuilder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public OrganizationBuilder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public OrganizationBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public OrganizationBuilder supportEmail(String supportEmail) {
            this.supportEmail = supportEmail;
            return this;
        }

        public OrganizationBuilder defaultTaxRate(Long defaultTaxRate) {
            this.defaultTaxRate = defaultTaxRate;
            return this;
        }

        public OrganizationBuilder invoicePrefix(String invoicePrefix) {
            this.invoicePrefix = invoicePrefix;
            return this;
        }

        public OrganizationBuilder invoiceFooter(String invoiceFooter) {
            this.invoiceFooter = invoiceFooter;
            return this;
        }

        public OrganizationBuilder companyWebsite(String companyWebsite) {
            this.companyWebsite = companyWebsite;
            return this;
        }

        public OrganizationBuilder companyAddress(String companyAddress) {
            this.companyAddress = companyAddress;
            return this;
        }

        public OrganizationBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public OrganizationBuilder users(List<User> users) {
            this.users = users;
            return this;
        }

        public OrganizationBuilder customers(List<Customer> customers) {
            this.customers = customers;
            return this;
        }

        public OrganizationBuilder plans(List<Plan> plans) {
            this.plans = plans;
            return this;
        }

        public Organization build() {
            Organization org = new Organization();
            org.setName(name);
            org.setSlug(slug);
            org.setLogoUrl(logoUrl);
            org.setTimezone(timezone);
            org.setCurrency(currency);
            org.setSupportEmail(supportEmail);
            org.setDefaultTaxRate(defaultTaxRate);
            org.setInvoicePrefix(invoicePrefix);
            org.setInvoiceFooter(invoiceFooter);
            org.setCompanyWebsite(companyWebsite);
            org.setCompanyAddress(companyAddress);
            org.setVersion(version);
            org.setUsers(users);
            org.setCustomers(customers);
            org.setPlans(plans);
            return org;
        }
    }
}
