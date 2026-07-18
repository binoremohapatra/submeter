package com.submeter.billing;

import com.submeter.entity.InvoiceLineItem;
import com.submeter.entity.PlanTier;
import com.submeter.entity.Subscription;
import com.submeter.entity.enums.PricingModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingCalculator {

    /**
     * Option trade-off documented:
     * - Graduated (Tiered) Pricing vs Volume Pricing.
     * This implementation uses Graduated Pricing (like income tax brackets),
     * where each unit is priced at the tier it falls into.
     * Volume Pricing would price ALL units at the rate of the final tier reached.
     * Graduated is fairer and standard for SaaS usage (e.g. AWS, Stripe).
     *
     * @param subscription The subscription being billed.
     * @param totalUsage   The aggregated usage count for the billing period (0 for FLAT plans).
     * @return List of invoice line items (unsaved entities).
     */
    public List<InvoiceLineItem> calculateLineItems(Subscription subscription, long totalUsage) {
        List<InvoiceLineItem> items = new ArrayList<>();

        if (subscription.getPlan().getPricingModel() == PricingModel.FLAT) {
            InvoiceLineItem item = InvoiceLineItem.builder()
                    .amount(subscription.getPlan().getFlatAmount())
                    .description("Flat fee")
                    .quantity(1L)
                    .unitAmount(subscription.getPlan().getFlatAmount())
                    .pricingModel(PricingModel.FLAT)
                    .build();
            items.add(item);
            return items;
        }

        // TIERED Pricing (Graduated)
        long remainingUsage = totalUsage;
        long previousTierUpTo = 0L;

        for (PlanTier tier : subscription.getPlan().getTiers()) {
            if (remainingUsage <= 0) {
                break; // No more usage to bill
            }

            long tierVolume;
            if (tier.getUpTo() == null) {
                // Open-ended final tier
                tierVolume = remainingUsage;
            } else {
                long maxUnitsInTier = tier.getUpTo() - previousTierUpTo;
                tierVolume = Math.min(remainingUsage, maxUnitsInTier);
                previousTierUpTo = tier.getUpTo();
            }

            long amount = (tierVolume * tier.getUnitAmount()) + tier.getFlatFee();

            InvoiceLineItem item = InvoiceLineItem.builder()
                    .amount(amount)
                    .description("Usage (Tier " + tier.getTierOrder() + ")")
                    .quantity(tierVolume)
                    .unitAmount(tier.getUnitAmount())
                    .pricingModel(PricingModel.TIERED)
                    .build();
            items.add(item);

            remainingUsage -= tierVolume;
        }

        return items;
    }

    /**
     * Calculates a prorated refund line item for a mid-cycle cancellation.
     * Formula: (Unused Seconds / Total Seconds) * Flat Amount.
     * Note: Returns a negative amount.
     */
    public InvoiceLineItem calculateProratedRefund(Subscription subscription) {
        if (subscription.getPlan().getPricingModel() != PricingModel.FLAT) {
            throw new IllegalArgumentException("Cannot prorate usage-based subscriptions.");
        }

        Instant start = subscription.getCurrentPeriodStart();
        Instant end = subscription.getCurrentPeriodEnd();
        Instant canceledAt = subscription.getCanceledAt();

        if (canceledAt == null || canceledAt.isAfter(end) || canceledAt.isBefore(start)) {
            throw new IllegalArgumentException("Cancellation date must be within the current billing period.");
        }

        long totalSeconds = ChronoUnit.SECONDS.between(start, end);
        long unusedSeconds = ChronoUnit.SECONDS.between(canceledAt, end);

        if (totalSeconds == 0) return null;

        double unusedRatio = (double) unusedSeconds / totalSeconds;
        long flatAmount = subscription.getPlan().getFlatAmount();
        
        // Negative amount for refund
        long refundAmount = -Math.round(flatAmount * unusedRatio);

        if (refundAmount >= 0) {
            return null;
        }

        return InvoiceLineItem.builder()
                .amount(refundAmount)
                .description("Prorated refund for unused time")
                .quantity(1L)
                .unitAmount(refundAmount)
                .pricingModel(PricingModel.FLAT)
                .build();
    }
}
