package com.submeter.api.dto;

import com.submeter.entity.Payment;
import com.submeter.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

public class PaymentDto {
    private final UUID id;
    private final long amountCents;
    private final String currency;
    private final PaymentStatus status;
    private final String razorpayOrderId;
    private final String razorpayPaymentId;
    private final Instant createdAt;

    public PaymentDto(UUID id, long amountCents, String currency, PaymentStatus status, String razorpayOrderId, String razorpayPaymentId, Instant createdAt) {
        this.id = id;
        this.amountCents = amountCents;
        this.currency = currency;
        this.status = status;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.createdAt = createdAt;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static PaymentDto fromEntity(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .amountCents(payment.getAmountCents())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    // Builder
    public static PaymentDtoBuilder builder() {
        return new PaymentDtoBuilder();
    }

    public static class PaymentDtoBuilder {
        private UUID id;
        private long amountCents;
        private String currency;
        private PaymentStatus status;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private Instant createdAt;

        public PaymentDtoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PaymentDtoBuilder amountCents(long amountCents) {
            this.amountCents = amountCents;
            return this;
        }

        public PaymentDtoBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public PaymentDtoBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentDtoBuilder razorpayOrderId(String razorpayOrderId) {
            this.razorpayOrderId = razorpayOrderId;
            return this;
        }

        public PaymentDtoBuilder razorpayPaymentId(String razorpayPaymentId) {
            this.razorpayPaymentId = razorpayPaymentId;
            return this;
        }

        public PaymentDtoBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PaymentDto build() {
            return new PaymentDto(id, amountCents, currency, status, razorpayOrderId, razorpayPaymentId, createdAt);
        }
    }
}
