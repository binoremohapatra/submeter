package com.submeter.entity.enums;

/**
 * Invoice lifecycle status.
 *
 * <pre>
 * DRAFT        → created by nightly billing job; not yet sent to customer.
 * OPEN         → sent to customer; awaiting payment.
 * PAID         → payment.captured webhook confirmed.
 * VOID         → cancelled (e.g. subscription deleted, plan migration, credit memo).
 * UNCOLLECTIBLE → declared unrecoverable after all retry attempts exhausted.
 * </pre>
 */
public enum InvoiceStatus {
    DRAFT,
    OPEN,
    PAID,
    VOID,
    UNCOLLECTIBLE
}
