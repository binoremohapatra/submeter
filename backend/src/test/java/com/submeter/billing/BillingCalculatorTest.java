package com.submeter.billing;

import com.submeter.entity.InvoiceLineItem;
import com.submeter.entity.Plan;
import com.submeter.entity.PlanTier;
import com.submeter.entity.Subscription;
import com.submeter.entity.enums.BillingInterval;
import com.submeter.entity.enums.PricingModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BillingCalculatorTest {

    private final BillingCalculator calculator = new BillingCalculator();

    // ── Test 1: Flat Pricing ──────────────────────────────────────────────────

    @Test
    @DisplayName("Flat plan generates exactly one line item with the flat amount")
    void calculate_flatPlan_generatesOneLineItem() {
        Plan plan = Plan.builder()
                .pricingModel(PricingModel.FLAT)
                .flatAmount(5000L) // 50.00
                .build();
        Subscription sub = Subscription.builder()
                .plan(plan)
                .build();

        List<InvoiceLineItem> items = calculator.calculateLineItems(sub, 0L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getAmount()).isEqualTo(5000L);
        assertThat(items.get(0).getDescription()).isEqualTo("Flat fee");
        assertThat(items.get(0).getQuantity()).isEqualTo(1L);
    }

    // ── Test 2: Tiered Pricing (Graduated) ────────────────────────────────────

    @Test
    @DisplayName("Tiered plan calculates across boundary (graduated pricing)")
    void calculate_tieredPlan_graduatedPricing() {
        // Tier 1: 1-100 units @ 10 paisa each, flat fee 500
        // Tier 2: 101-500 units @ 8 paisa each, flat fee 1000
        // Tier 3: 501+ units @ 5 paisa each, flat fee 2000
        Plan plan = Plan.builder()
                .pricingModel(PricingModel.TIERED)
                .tiers(List.of(
                        PlanTier.builder().tierOrder(1).upTo(100L).unitAmount(10L).flatFee(500L).build(),
                        PlanTier.builder().tierOrder(2).upTo(500L).unitAmount(8L).flatFee(1000L).build(),
                        PlanTier.builder().tierOrder(3).upTo(null).unitAmount(5L).flatFee(2000L).build()
                ))
                .build();
        Subscription sub = Subscription.builder()
                .plan(plan)
                .build();

        // 150 units should cross into Tier 2.
        // Tier 1: 100 units * 10 = 1000 + 500 flat = 1500
        // Tier 2: 50 units * 8 = 400 + 1000 flat = 1400
        // Total expected = 2900 (across 2 line items)
        List<InvoiceLineItem> items = calculator.calculateLineItems(sub, 150L);

        assertThat(items).hasSize(2);
        
        assertThat(items.get(0).getQuantity()).isEqualTo(100L);
        assertThat(items.get(0).getAmount()).isEqualTo(1500L); // 100*10 + 500

        assertThat(items.get(1).getQuantity()).isEqualTo(50L);
        assertThat(items.get(1).getAmount()).isEqualTo(1400L); // 50*8 + 1000
    }

    // ── Test 3: Proration (Mid-cycle Cancel) ──────────────────────────────────

    @Test
    @DisplayName("Annual plan canceled mid-cycle calculates prorated refund")
    void calculateProration_annualPlan_refundsUnusedTime() {
        Plan plan = Plan.builder()
                .pricingModel(PricingModel.FLAT)
                .flatAmount(120000L) // 1200.00 per year
                .billingInterval(BillingInterval.ANNUAL)
                .build();

        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2027-01-01T00:00:00Z");
        
        // Canceled exactly 1/4 of the way through the year
        long totalSeconds = ChronoUnit.SECONDS.between(start, end);
        Instant canceledAt = start.plusSeconds(totalSeconds / 4);

        Subscription sub = Subscription.builder()
                .plan(plan)
                .currentPeriodStart(start)
                .currentPeriodEnd(end)
                .canceledAt(canceledAt)
                .build();

        // Expected refund is roughly 75% of 120000 = 90000 (negative)
        InvoiceLineItem refundItem = calculator.calculateProratedRefund(sub);

        assertThat(refundItem.getAmount()).isEqualTo(-90000L);
        assertThat(refundItem.getDescription()).contains("Prorated refund");
    }
}
