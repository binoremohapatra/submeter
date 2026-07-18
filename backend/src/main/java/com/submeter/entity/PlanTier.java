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

/**
 * A single pricing tier within a TIERED or METERED plan.
 *
 * <p>Tier application rules (implemented in {@code BillingCalculator}, Milestone 4):
 * <ul>
 *   <li>Tiers are applied in ascending {@code tierOrder}.</li>
 *   <li>For TIERED pricing (graduated), each unit is priced at the rate of
 *       the tier it falls into. Units 1–{@code tier1.upTo} at {@code tier1.unitAmount},
 *       units {@code tier1.upTo+1}–{@code tier2.upTo} at {@code tier2.unitAmount}, etc.</li>
 *   <li>The last tier ({@code upTo == null}) has no upper bound.</li>
 *   <li>{@code flatFee} is added once per tier that is "entered", Stripe-style.</li>
 * </ul>
 *
 * <p>Tiers are append-only once a subscription references the owning plan version.
 * A plan version bump replaces the entire tier list via {@code CascadeType.ALL}
 * on {@link Plan#getTiers()}.
 *
 * <p>FLAG: {@code unitAmount} NOT NULL — a null unit amount would silently produce
 * ₹0 invoice line items for metered usage.
 */
@Entity
@Table(name = "plan_tiers")
@NoArgsConstructor
@AllArgsConstructor
public class PlanTier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private Plan plan;

    /**
     * 1-indexed position in the tier sequence. Unique per plan (enforced by DB UNIQUE constraint).
     * The {@link Plan} entity uses {@code @OrderBy("tierOrder ASC")} to ensure correct ordering.
     */
    @Column(name = "tier_order", nullable = false, updatable = false)
    private int tierOrder;

    /**
     * Inclusive upper bound for this tier (unit count, not amount).
     * {@code null} means this is the last (open-ended) tier.
     */
    @Column(name = "up_to")
    private Long upTo;

    /**
     * Price per unit within this tier, in paisa.
     * FLAG: NOT NULL — see class Javadoc.
     */
    @Column(name = "unit_amount", nullable = false, updatable = false)
    private long unitAmount;

    /**
     * Per-tier flat fee in paisa added once when this tier is entered.
     * 0 for plans that don't use Stripe-style per-tier flat fees.
     * FLAG: NOT NULL DEFAULT 0 — null would require null-safe arithmetic in billing.
     */
    @Column(name = "flat_fee", nullable = false, updatable = false)
    private long flatFee = 0L;

    // Getters and Setters
    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public int getTierOrder() {
        return tierOrder;
    }

    public void setTierOrder(int tierOrder) {
        this.tierOrder = tierOrder;
    }

    public Long getUpTo() {
        return upTo;
    }

    public void setUpTo(Long upTo) {
        this.upTo = upTo;
    }

    public long getUnitAmount() {
        return unitAmount;
    }

    public void setUnitAmount(long unitAmount) {
        this.unitAmount = unitAmount;
    }

    public long getFlatFee() {
        return flatFee;
    }

    public void setFlatFee(long flatFee) {
        this.flatFee = flatFee;
    }

    // Builder
    public static PlanTierBuilder builder() {
        return new PlanTierBuilder();
    }

    public static class PlanTierBuilder {
        private Plan plan;
        private int tierOrder;
        private Long upTo;
        private long unitAmount;
        private long flatFee = 0L;

        public PlanTierBuilder plan(Plan plan) {
            this.plan = plan;
            return this;
        }

        public PlanTierBuilder tierOrder(int tierOrder) {
            this.tierOrder = tierOrder;
            return this;
        }

        public PlanTierBuilder upTo(Long upTo) {
            this.upTo = upTo;
            return this;
        }

        public PlanTierBuilder unitAmount(long unitAmount) {
            this.unitAmount = unitAmount;
            return this;
        }

        public PlanTierBuilder flatFee(long flatFee) {
            this.flatFee = flatFee;
            return this;
        }

        public PlanTier build() {
            PlanTier tier = new PlanTier();
            tier.setPlan(plan);
            tier.setTierOrder(tierOrder);
            tier.setUpTo(upTo);
            tier.setUnitAmount(unitAmount);
            tier.setFlatFee(flatFee);
            return tier;
        }
    }
}
