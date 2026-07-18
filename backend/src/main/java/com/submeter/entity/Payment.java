package com.submeter.entity;

import com.submeter.entity.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * A Razorpay payment attempt against an invoice.
 *
 * <p>One payment row is created per POST /invoices/:id/pay call (Razorpay order creation).
 * Multiple payment rows per invoice are possible if the first attempt fails and the
 * customer retries — each Razorpay order gets its own row.
 *
 * <p>Webhook idempotency (Milestone 5):
 * The {@code razorpayPaymentId} field has a nullable-safe partial unique index
 * ({@code uq_payments_razorpay_payment_id} in V2 migration) that enforces:
 * "if a razorpay_payment_id is set, it is globally unique in the payments table."
 * A duplicate webhook for the same payment_id will fail the INSERT and be handled
 * as a no-op in the webhook handler.
 *
 * <p>FLAG: {@code razorpayOrderId} NOT NULL — created before the payment attempt;
 * always known by the time the row is inserted.
 * FLAG: {@code amountCents} NOT NULL — a null amount would create a ₹0 Razorpay order
 * without raising an error.
 * FLAG: {@code currency} NOT NULL DEFAULT 'INR' — Razorpay test mode is INR only.
 */
@Entity
@Table(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    /** Razorpay order ID returned by the Orders API. Set at creation. */
    @Column(name = "razorpay_order_id", nullable = false, updatable = false)
    private String razorpayOrderId;

    /**
     * Razorpay payment ID set when the {@code payment.captured} webhook fires.
     * Null until then. The partial unique index prevents duplicate webhook processing.
     */
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    /** Invoice total in paisa at the time of order creation. */
    @Column(name = "amount_cents", nullable = false, updatable = false)
    private long amountCents;

    /** Currency code. Always "INR" in v1 (Razorpay test mode constraint). */
    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Razorpay failure description or internal error message.
     * e.g. {@code "Card declined by issuer"}, {@code "Webhook signature mismatch"}.
     * Set only when {@code status == FAILED}.
     */
    @Column(name = "failure_reason")
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private com.submeter.entity.enums.PaymentMethod paymentMethod;

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public com.submeter.entity.enums.PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(com.submeter.entity.enums.PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // Builder
    public static PaymentBuilder builder() {
        return new PaymentBuilder();
    }

    public static class PaymentBuilder {
        private Organization organization;
        private Invoice invoice;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private long amountCents;
        private String currency = "INR";
        private PaymentStatus status = PaymentStatus.PENDING;
        private String failureReason;
        private com.submeter.entity.enums.PaymentMethod paymentMethod;

        public PaymentBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public PaymentBuilder invoice(Invoice invoice) {
            this.invoice = invoice;
            return this;
        }

        public PaymentBuilder razorpayOrderId(String razorpayOrderId) {
            this.razorpayOrderId = razorpayOrderId;
            return this;
        }

        public PaymentBuilder razorpayPaymentId(String razorpayPaymentId) {
            this.razorpayPaymentId = razorpayPaymentId;
            return this;
        }

        public PaymentBuilder amountCents(long amountCents) {
            this.amountCents = amountCents;
            return this;
        }

        public PaymentBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public PaymentBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public PaymentBuilder paymentMethod(com.submeter.entity.enums.PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Payment build() {
            Payment payment = new Payment();
            payment.setOrganization(organization);
            payment.setInvoice(invoice);
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setAmountCents(amountCents);
            payment.setCurrency(currency);
            payment.setStatus(status);
            payment.setFailureReason(failureReason);
            payment.setPaymentMethod(paymentMethod);
            return payment;
        }
    }
}
