package com.submeter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all SubMeter entities.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@code id}         — UUID PK, generated in Java via {@link GenerationType#UUID}
 *                            (UUID4). The migration's {@code DEFAULT gen_random_uuid()}
 *                            is a fallback for direct SQL inserts (seed script).</li>
 *   <li>{@code createdAt}  — set once on INSERT by Hibernate's {@code @CreationTimestamp};
 *                            the DB trigger in V3 is a fallback for raw SQL.</li>
 *   <li>{@code updatedAt}  — maintained by Hibernate's {@code @UpdateTimestamp} on every
 *                            merge; the V3 DB trigger provides the same guarantee for
 *                            direct SQL updates.</li>
 * </ul>
 *
 * <p><strong>NOT intended to be used directly</strong> — always extend
 * {@link SoftDeletableEntity} for entities that support soft-delete,
 * or this class for entities that do not.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false,
            columnDefinition = "uuid")
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
