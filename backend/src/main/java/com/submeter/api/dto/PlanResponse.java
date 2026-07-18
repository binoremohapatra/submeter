package com.submeter.api.dto;

import com.submeter.entity.Plan;
import com.submeter.entity.PlanTier;
import com.submeter.entity.enums.BillingInterval;
import com.submeter.entity.enums.PricingModel;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class PlanResponse {

    private final UUID id;
    private final String name;
    private final String description;
    private final PricingModel pricingModel;
    private final BillingInterval billingInterval;
    private final Long flatAmount;
    private final int trialDays;
    private final boolean archived;
    private final int version;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<PlanTierDto> tiers;

    public PlanResponse(UUID id, String name, String description, PricingModel pricingModel, BillingInterval billingInterval, Long flatAmount, int trialDays, boolean archived, int version, Instant createdAt, Instant updatedAt, List<PlanTierDto> tiers) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pricingModel = pricingModel;
        this.billingInterval = billingInterval;
        this.flatAmount = flatAmount;
        this.trialDays = trialDays;
        this.archived = archived;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tiers = tiers;
    }

    public static PlanResponse fromEntity(Plan plan) {
        List<PlanTierDto> tierDtos = null;
        if (plan.getTiers() != null && !plan.getTiers().isEmpty()) {
            tierDtos = plan.getTiers().stream()
                    .map(t -> PlanTierDto.builder()
                            .upTo(t.getUpTo())
                            .flatFee(t.getFlatFee())
                            .unitAmount(t.getUnitAmount())
                            .build())
                    .collect(Collectors.toList());
        }

        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .pricingModel(plan.getPricingModel())
                .billingInterval(plan.getBillingInterval())
                .flatAmount(plan.getFlatAmount())
                .trialDays(plan.getTrialDays())
                .archived(plan.isArchived())
                .version(plan.getVersion())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .tiers(tierDtos)
                .build();
    }

    // Builder
    public static PlanResponseBuilder builder() {
        return new PlanResponseBuilder();
    }

    public static class PlanResponseBuilder {
        private UUID id;
        private String name;
        private String description;
        private PricingModel pricingModel;
        private BillingInterval billingInterval;
        private Long flatAmount;
        private int trialDays;
        private boolean archived;
        private int version;
        private Instant createdAt;
        private Instant updatedAt;
        private List<PlanTierDto> tiers;

        public PlanResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PlanResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PlanResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PlanResponseBuilder pricingModel(PricingModel pricingModel) {
            this.pricingModel = pricingModel;
            return this;
        }

        public PlanResponseBuilder billingInterval(BillingInterval billingInterval) {
            this.billingInterval = billingInterval;
            return this;
        }

        public PlanResponseBuilder flatAmount(Long flatAmount) {
            this.flatAmount = flatAmount;
            return this;
        }

        public PlanResponseBuilder trialDays(int trialDays) {
            this.trialDays = trialDays;
            return this;
        }

        public PlanResponseBuilder archived(boolean archived) {
            this.archived = archived;
            return this;
        }

        public PlanResponseBuilder version(int version) {
            this.version = version;
            return this;
        }

        public PlanResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PlanResponseBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public PlanResponseBuilder tiers(List<PlanTierDto> tiers) {
            this.tiers = tiers;
            return this;
        }

        public PlanResponse build() {
            return new PlanResponse(id, name, description, pricingModel, billingInterval, flatAmount, trialDays, archived, version, createdAt, updatedAt, tiers);
        }
    }
}
