package com.submeter.entity.enums;

/**
 * Audit log action type. Covers all mutation verbs across all entities.
 */
public enum AuditAction {
    /** A new entity was created. diff = the full initial state. */
    CREATE,

    /** An entity's fields were updated. diff = {before: {...}, after: {...}} */
    UPDATE,

    /** An entity was soft-deleted. diff = {deleted_at: "..."} */
    DELETE,

    /**
     * A subscription or invoice status changed.
     * diff = {from: "TRIAL", to: "ACTIVE"}
     * Used instead of UPDATE to make status transitions queryable independently.
     */
    STATUS_CHANGE
}
