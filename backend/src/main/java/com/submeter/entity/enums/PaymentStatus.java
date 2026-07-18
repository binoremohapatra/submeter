package com.submeter.entity.enums;

/**
 * Payment attempt status, driven by Razorpay webhooks.
 *
 * <pre>
 * PENDING  → Razorpay order created; awaiting customer action.
 * SUCCESS  → payment.captured webhook received and verified.
 * FAILED   → payment.failed webhook received; failure_reason populated.
 * REFUNDED → payment.refunded webhook received (future; not in v1 scope).
 * </pre>
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
