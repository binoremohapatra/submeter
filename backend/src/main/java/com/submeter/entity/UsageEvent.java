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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A metered usage event ingested via {@code POST /usage-events}.
 *
 * <p>Idempotency: the {@code idempotencyKey} column has a DB UNIQUE constraint
 * (V1 migration) so duplicate deliveries (network retry, client bug) are rejected
 * silently with HTTP 200 on the second call — the first write wins.
 *
 * <p>Timestamps:
 * <ul>
 *   <li>{@code occurredAt} — client-provided; when the event actually happened.
 *       May be in the past (back-filling is allowed within the current billing period).
 *       The billing engine groups events by the period they fall into using this field.</li>
 *   <li>{@code createdAt}  — from {@link BaseEntity}; when the API received and persisted
 *       the event. Used as the "processed at" timestamp in the audit log.</li>
 * </ul>
 *
 * <p>Cancellation behavior:
 * <ul>
 *   <li>Events for ACTIVE subscriptions: accepted (status = OK).</li>
 *   <li>Events for CANCELED subscriptions: stored with {@code rejectedReason = "subscription_canceled"};
 *       never rolled into an invoice. Kept for audit trail — never deleted.</li>
 *   <li>Events with {@code invoice_id} set: already billed; immutable.</li>
 * </ul>
 *
 * <p>FLAG: {@code quantity > 0} CHECK — a zero or negative quantity is meaningless for metering
 * and would silently reduce invoice totals if it bypassed the service-layer validation.
 * FLAG: {@code idempotencyKey} NOT NULL UNIQUE — client must always provide one; recommended format is UUID4.
 */
@Entity
@Table(name = "usage_events")
@NoArgsConstructor
@AllArgsConstructor
public class UsageEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false, updatable = false)
    private Subscription subscription;

    /**
     * Event type identifier matching a meter defined on the plan.
     * e.g. {@code "api_calls"}, {@code "storage_gb"}.
     * Validated at ingestion time against the plan's event types.
     */
    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    /** Number of units consumed. Must be > 0. */
    @Column(name = "quantity", nullable = false, updatable = false)
    private long quantity;

    /**
     * Client-provided deduplication key. UUID4 recommended.
     * The DB UNIQUE constraint ensures idempotency at the storage layer.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private String idempotencyKey;

    /**
     * When the event occurred in the customer's system.
     * The billing engine uses this (not {@code createdAt}) to place the event
     * in the correct billing period.
     */
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /**
     * Set by the billing job when this event is rolled into a draft invoice.
     * Null means the event has not yet been billed.
     * The partial index {@code idx_usage_events_unbilled} covers the un-billed case.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    /**
     * Set when the event is rejected after the subscription is canceled.
     * e.g. {@code "subscription_canceled"}.
     * A non-null value means this event will never be billed.
     */
    @Column(name = "rejected_reason")
    private String rejectedReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Builder
    public static UsageEventBuilder builder() {
        return new UsageEventBuilder();
    }

    public static class UsageEventBuilder {
        private Organization organization;
        private Subscription subscription;
        private String eventType;
        private long quantity;
        private String idempotencyKey;
        private Instant occurredAt;
        private Invoice invoice;
        private String rejectedReason;
        private Map<String, Object> metadata = new HashMap<>();

        public UsageEventBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public UsageEventBuilder subscription(Subscription subscription) {
            this.subscription = subscription;
            return this;
        }

        public UsageEventBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public UsageEventBuilder quantity(long quantity) {
            this.quantity = quantity;
            return this;
        }

        public UsageEventBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public UsageEventBuilder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public UsageEventBuilder invoice(Invoice invoice) {
            this.invoice = invoice;
            return this;
        }

        public UsageEventBuilder rejectedReason(String rejectedReason) {
            this.rejectedReason = rejectedReason;
            return this;
        }

        public UsageEventBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public UsageEvent build() {
            UsageEvent event = new UsageEvent();
            event.setOrganization(organization);
            event.setSubscription(subscription);
            event.setEventType(eventType);
            event.setQuantity(quantity);
            event.setIdempotencyKey(idempotencyKey);
            event.setOccurredAt(occurredAt);
            event.setInvoice(invoice);
            event.setRejectedReason(rejectedReason);
            event.setMetadata(metadata);
            return event;
        }
    }
}
