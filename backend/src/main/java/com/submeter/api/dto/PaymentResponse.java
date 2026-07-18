package com.submeter.api.dto;

import lombok.Builder;
import lombok.Getter;

public class PaymentResponse {
    private final String razorpayOrderId;
    private final long amountCents;
    private final String currency;

    public PaymentResponse(String razorpayOrderId, long amountCents, String currency) {
        this.razorpayOrderId = razorpayOrderId;
        this.amountCents = amountCents;
        this.currency = currency;
    }

    // Getters
    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    // Builder
    public static PaymentResponseBuilder builder() {
        return new PaymentResponseBuilder();
    }

    public static class PaymentResponseBuilder {
        private String razorpayOrderId;
        private long amountCents;
        private String currency;

        public PaymentResponseBuilder razorpayOrderId(String razorpayOrderId) {
            this.razorpayOrderId = razorpayOrderId;
            return this;
        }

        public PaymentResponseBuilder amountCents(long amountCents) {
            this.amountCents = amountCents;
            return this;
        }

        public PaymentResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public PaymentResponse build() {
            return new PaymentResponse(razorpayOrderId, amountCents, currency);
        }
    }
}
