package com.submeter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Extension of {@link BaseEntity} for entities that support soft-delete.
 *
 * <p>Soft-delete semantics:
 * <ul>
 *   <li>A null {@code deletedAt} means the record is active.</li>
 *   <li>A non-null {@code deletedAt} means the record is logically deleted.</li>
 *   <li>All repository queries MUST filter {@code WHERE deleted_at IS NULL}
 *       unless explicitly querying deleted records (e.g. admin recovery view).
 *       This filter is enforced at the repository layer, NOT via
 *       {@code @Where} or {@code @SQLRestriction} — Hibernate's filter
 *       annotations can be bypassed by native queries, so we make it explicit.</li>
 * </ul>
 *
 * <p>Entities using this: {@link Organization}, {@link User}, {@link Customer}.
 */
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Getters and Setters
    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * Convenience method; prefer checking {@code deletedAt != null} in queries
     * rather than calling this (it won't be visible to JPQL/Criteria).
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Sets deleted_at to now; does NOT cascade — call from service layer. */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }
}
