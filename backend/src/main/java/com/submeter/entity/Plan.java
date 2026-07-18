package com.submeter.entity;

import com.submeter.entity.enums.BillingInterval;
import com.submeter.entity.enums.PricingModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Pricing template that subscriptions are attached to.
 *
 * <p><strong>Pricing immutability contract:</strong>
 * Once a subscription references a plan, its pricing is frozen via
 * {@code plan_version} on the subscription. "Changing" a plan's price
 * increments {@code version} and replaces {@code tiers} (or {@code flatAmount})
 * — existing subscriptions continue to use the old version, new subscriptions
 * get the new version. This is enforced at the service layer.
 *
 * <p>Only metadata fields ({@code name}, {@code description}) may be updated
 * in-place after subscribers exist. Any price-affecting field change must go
 * through the version-bump path.
 *
 * <p>Plans are not soft-deleted. Instead, {@code isArchived = true} prevents
 * new subscriptions while existing ones continue unaffected.
 *
 * <p>FLAG: {@code flatAmount} has a DB CHECK constraint (V1) ensuring it is
 * NOT NULL when {@code pricingModel = FLAT}. The service layer must also
 * validate this before persisting to produce a clear error message.
 */
@Entity
@Table(name = "plans")
@NoArgsConstructor
@AllArgsConstructor
public class Plan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false, length = 10, updatable = false)
    private PricingModel pricingModel;

    /**
     * Fixed charge per billing period in paisa.
     * NOT NULL when {@code pricingModel == FLAT} — enforced by DB CHECK + service validation.
     * NULL for TIERED/METERED (pricing comes from {@link #tiers}).
     */
    @Column(name = "flat_amount")
    private Long flatAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 10, updatable = false)
    private BillingInterval billingInterval;

    /**
     * Number of trial days. 0 means no trial (subscription starts ACTIVE immediately).
     * FLAG: NOT NULL DEFAULT 0 — a null would require null-safe trial logic everywhere.
     */
    @Column(name = "trial_days", nullable = false)
    private int trialDays = 0;

    /**
     * Archived plans accept no new subscriptions.
     * Existing subscriptions continue at their locked {@code plan_version}.
     */
    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;

    /**
     * Incremented each time a price-affecting change is made.
     * Existing subscriptions lock in their version at creation time.
     * FLAG: NOT NULL DEFAULT 1 — zero version is meaningless and would silently
     * cause subscriptions to use incorrect pricing.
     */
    @Column(name = "version", nullable = false)
    private int version = 1;

    /**
     * Tier definitions for TIERED/METERED plans.
     * Ordered ascending by {@code tierOrder}; the last tier has {@code upTo = null} (∞).
     * CascadeType.ALL + orphanRemoval: replacing tiers on a version bump is a single
     * operation — remove all, add new ones.
     */
    @OneToMany(
            mappedBy = "plan",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("tierOrder ASC")
    private List<PlanTier> tiers = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PricingModel getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(PricingModel pricingModel) {
        this.pricingModel = pricingModel;
    }

    public Long getFlatAmount() {
        return flatAmount;
    }

    public void setFlatAmount(Long flatAmount) {
        this.flatAmount = flatAmount;
    }

    public BillingInterval getBillingInterval() {
        return billingInterval;
    }

    public void setBillingInterval(BillingInterval billingInterval) {
        this.billingInterval = billingInterval;
    }

    public int getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(int trialDays) {
        this.trialDays = trialDays;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<PlanTier> getTiers() {
        return tiers;
    }

    public void setTiers(List<PlanTier> tiers) {
        this.tiers = tiers;
    }

    // Builder
    public static PlanBuilder builder() {
        return new PlanBuilder();
    }

    public static class PlanBuilder {
        private Organization organization;
        private String name;
        private String description;
        private PricingModel pricingModel;
        private Long flatAmount;
        private BillingInterval billingInterval;
        private int trialDays = 0;
        private boolean archived = false;
        private int version = 1;
        private List<PlanTier> tiers = new ArrayList<>();

        public PlanBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public PlanBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PlanBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PlanBuilder pricingModel(PricingModel pricingModel) {
            this.pricingModel = pricingModel;
            return this;
        }

        public PlanBuilder flatAmount(Long flatAmount) {
            this.flatAmount = flatAmount;
            return this;
        }

        public PlanBuilder billingInterval(BillingInterval billingInterval) {
            this.billingInterval = billingInterval;
            return this;
        }

        public PlanBuilder trialDays(int trialDays) {
            this.trialDays = trialDays;
            return this;
        }

        public PlanBuilder archived(boolean archived) {
            this.archived = archived;
            return this;
        }

        public PlanBuilder version(int version) {
            this.version = version;
            return this;
        }

        public PlanBuilder tiers(List<PlanTier> tiers) {
            this.tiers = tiers;
            return this;
        }

        public Plan build() {
            Plan plan = new Plan();
            plan.setOrganization(organization);
            plan.setName(name);
            plan.setDescription(description);
            plan.setPricingModel(pricingModel);
            plan.setFlatAmount(flatAmount);
            plan.setBillingInterval(billingInterval);
            plan.setTrialDays(trialDays);
            plan.setArchived(archived);
            plan.setVersion(version);
            plan.setTiers(tiers);
            return plan;
        }
    }
}
