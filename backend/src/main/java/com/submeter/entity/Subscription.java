package com.submeter.entity;

import com.submeter.entity.enums.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Links a Customer to a Plan and tracks the billing lifecycle.
 *
 * <p>State machine transitions (see {@code SubscriptionStatus} for the full graph):
 * <ul>
 *   <li>On creation:          status = TRIAL (if plan.trialDays > 0), else ACTIVE.</li>
 *   <li>Nightly job (M4):     TRIAL → ACTIVE when trialEndAt &lt;= now().</li>
 *   <li>Razorpay webhook:     ACTIVE → PAST_DUE on payment.failed.</li>
 *   <li>Retry payment:        PAST_DUE → ACTIVE on payment.captured.</li>
 *   <li>Cancel API:           ACTIVE | TRIAL | PAST_DUE → CANCELED.</li>
 *   <li>Retry window expired: PAST_DUE → CANCELED (nightly job).</li>
 * </ul>
 * Invalid transitions return HTTP 422 with a machine-readable reason code.
 *
 * <p><strong>Plan version contract:</strong> {@code planVersion} is set at creation
 * time from {@code plan.version}. All billing calculations (M4) use the tiers and
 * flat amounts that were active at that version, not the current plan state.
 *
 * <p>FLAG: {@code planVersion} NOT NULL — without it, a plan price change would
 * silently re-price existing subscriptions retroactively.
 * FLAG: {@code org_id} is denormalized here (reachable via customer → organization)
 * for query performance — every subscription list query can filter by org_id directly
 * without a join to customers.
 */
@Entity
@Table(name = "subscriptions")
@NoArgsConstructor
@AllArgsConstructor
public class Subscription extends BaseEntity {

    /**
     * Denormalized for query scoping. Must equal {@code customer.organization.id}.
     * Service layer validates consistency on create.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private Plan plan;

    /**
     * Snapshot of {@code plan.version} at subscription creation time.
     * The billing engine uses this to look up the correct tier configuration.
     */
    @Column(name = "plan_version", nullable = false, updatable = false)
    private int planVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    /** Non-null when plan.trialDays > 0. Nightly job promotes TRIAL → ACTIVE on expiry. */
    @Column(name = "trial_end_at")
    private Instant trialEndAt;

    /** Set when status transitions to ACTIVE; the billing period start anchor. */
    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    /** Set when status transitions to ACTIVE; the nightly job invoices when this approaches. */
    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    /** Set when status transitions to CANCELED. Immutable thereafter. */
    @Column(name = "canceled_at")
    private Instant canceledAt;

    /**
     * Human-readable or machine-readable reason.
     * e.g. {@code "customer_deleted"}, {@code "retry_window_expired"}, {@code "admin_cancel"}.
     */
    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "months_active")
    private Integer monthsActive;

    @OneToMany(mappedBy = "subscription", fetch = FetchType.LAZY)
    private List<Invoice> invoices = new ArrayList<>();

    @OneToMany(mappedBy = "subscription", fetch = FetchType.LAZY)
    private List<UsageEvent> usageEvents = new ArrayList<>();

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public int getPlanVersion() {
        return planVersion;
    }

    public void setPlanVersion(int planVersion) {
        this.planVersion = planVersion;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public Instant getTrialEndAt() {
        return trialEndAt;
    }

    public void setTrialEndAt(Instant trialEndAt) {
        this.trialEndAt = trialEndAt;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public void setCurrentPeriodStart(Instant currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(Instant canceledAt) {
        this.canceledAt = canceledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Integer getMonthsActive() {
        return monthsActive;
    }

    public void setMonthsActive(Integer monthsActive) {
        this.monthsActive = monthsActive;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
    }

    public List<UsageEvent> getUsageEvents() {
        return usageEvents;
    }

    public void setUsageEvents(List<UsageEvent> usageEvents) {
        this.usageEvents = usageEvents;
    }

    // Builder
    public static SubscriptionBuilder builder() {
        return new SubscriptionBuilder();
    }

    public static class SubscriptionBuilder {
        private Organization organization;
        private Customer customer;
        private Plan plan;
        private int planVersion;
        private SubscriptionStatus status;
        private Instant trialEndAt;
        private Instant currentPeriodStart;
        private Instant currentPeriodEnd;
        private Instant canceledAt;
        private String cancellationReason;
        private Integer monthsActive;
        private List<Invoice> invoices = new ArrayList<>();
        private List<UsageEvent> usageEvents = new ArrayList<>();

        public SubscriptionBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public SubscriptionBuilder customer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public SubscriptionBuilder plan(Plan plan) {
            this.plan = plan;
            return this;
        }

        public SubscriptionBuilder planVersion(int planVersion) {
            this.planVersion = planVersion;
            return this;
        }

        public SubscriptionBuilder status(SubscriptionStatus status) {
            this.status = status;
            return this;
        }

        public SubscriptionBuilder trialEndAt(Instant trialEndAt) {
            this.trialEndAt = trialEndAt;
            return this;
        }

        public SubscriptionBuilder currentPeriodStart(Instant currentPeriodStart) {
            this.currentPeriodStart = currentPeriodStart;
            return this;
        }

        public SubscriptionBuilder currentPeriodEnd(Instant currentPeriodEnd) {
            this.currentPeriodEnd = currentPeriodEnd;
            return this;
        }

        public SubscriptionBuilder canceledAt(Instant canceledAt) {
            this.canceledAt = canceledAt;
            return this;
        }

        public SubscriptionBuilder cancellationReason(String cancellationReason) {
            this.cancellationReason = cancellationReason;
            return this;
        }

        public SubscriptionBuilder monthsActive(Integer monthsActive) {
            this.monthsActive = monthsActive;
            return this;
        }

        public SubscriptionBuilder invoices(List<Invoice> invoices) {
            this.invoices = invoices;
            return this;
        }

        public SubscriptionBuilder usageEvents(List<UsageEvent> usageEvents) {
            this.usageEvents = usageEvents;
            return this;
        }

        public Subscription build() {
            Subscription sub = new Subscription();
            sub.setOrganization(organization);
            sub.setCustomer(customer);
            sub.setPlan(plan);
            sub.setPlanVersion(planVersion);
            sub.setStatus(status);
            sub.setTrialEndAt(trialEndAt);
            sub.setCurrentPeriodStart(currentPeriodStart);
            sub.setCurrentPeriodEnd(currentPeriodEnd);
            sub.setCanceledAt(canceledAt);
            sub.setCancellationReason(cancellationReason);
            sub.setMonthsActive(monthsActive);
            sub.setInvoices(invoices);
            sub.setUsageEvents(usageEvents);
            return sub;
        }
    }
}
