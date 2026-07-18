package com.submeter.api.dto;

import com.submeter.entity.Subscription;
import com.submeter.entity.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class SubscriptionResponse {
    private final UUID id;
    private final UUID customerId;
    private final String customerName;
    private final UUID planId;
    private final String planName;
    private final int planVersion;
    private final SubscriptionStatus status;
    private final Instant currentPeriodStart;
    private final Instant currentPeriodEnd;
    private final Instant trialEndAt;
    private final Instant canceledAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public SubscriptionResponse(UUID id, UUID customerId, String customerName, UUID planId, String planName, int planVersion, SubscriptionStatus status, Instant currentPeriodStart, Instant currentPeriodEnd, Instant trialEndAt, Instant canceledAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.planId = planId;
        this.planName = planName;
        this.planVersion = planVersion;
        this.status = status;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.trialEndAt = trialEndAt;
        this.canceledAt = canceledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SubscriptionResponse fromEntity(Subscription sub) {
        return SubscriptionResponse.builder()
                .id(sub.getId())
                .customerId(sub.getCustomer().getId())
                .customerName(sub.getCustomer().getName())
                .planId(sub.getPlan().getId())
                .planName(sub.getPlan().getName())
                .planVersion(sub.getPlanVersion())
                .status(sub.getStatus())
                .currentPeriodStart(sub.getCurrentPeriodStart())
                .currentPeriodEnd(sub.getCurrentPeriodEnd())
                .trialEndAt(sub.getTrialEndAt())
                .canceledAt(sub.getCanceledAt())
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }

    // Builder
    public static SubscriptionResponseBuilder builder() {
        return new SubscriptionResponseBuilder();
    }

    public static class SubscriptionResponseBuilder {
        private UUID id;
        private UUID customerId;
        private String customerName;
        private UUID planId;
        private String planName;
        private int planVersion;
        private SubscriptionStatus status;
        private Instant currentPeriodStart;
        private Instant currentPeriodEnd;
        private Instant trialEndAt;
        private Instant canceledAt;
        private Instant createdAt;
        private Instant updatedAt;

        public SubscriptionResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public SubscriptionResponseBuilder customerId(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        public SubscriptionResponseBuilder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public SubscriptionResponseBuilder planId(UUID planId) {
            this.planId = planId;
            return this;
        }

        public SubscriptionResponseBuilder planName(String planName) {
            this.planName = planName;
            return this;
        }

        public SubscriptionResponseBuilder planVersion(int planVersion) {
            this.planVersion = planVersion;
            return this;
        }

        public SubscriptionResponseBuilder status(SubscriptionStatus status) {
            this.status = status;
            return this;
        }

        public SubscriptionResponseBuilder currentPeriodStart(Instant currentPeriodStart) {
            this.currentPeriodStart = currentPeriodStart;
            return this;
        }

        public SubscriptionResponseBuilder currentPeriodEnd(Instant currentPeriodEnd) {
            this.currentPeriodEnd = currentPeriodEnd;
            return this;
        }

        public SubscriptionResponseBuilder trialEndAt(Instant trialEndAt) {
            this.trialEndAt = trialEndAt;
            return this;
        }

        public SubscriptionResponseBuilder canceledAt(Instant canceledAt) {
            this.canceledAt = canceledAt;
            return this;
        }

        public SubscriptionResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SubscriptionResponseBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SubscriptionResponse build() {
            return new SubscriptionResponse(id, customerId, customerName, planId, planName, planVersion, status, currentPeriodStart, currentPeriodEnd, trialEndAt, canceledAt, createdAt, updatedAt);
        }
    }
}
