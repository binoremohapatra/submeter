package com.submeter.repository;

import com.submeter.entity.Plan;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    @Query("""
        SELECT p FROM Plan p
        LEFT JOIN FETCH p.tiers
        WHERE p.id = :id AND p.organization.id = :orgId
    """)
    Optional<Plan> findByIdAndOrganizationIdWithTiers(UUID id, UUID orgId);

    // ── Keyset Pagination (Cursor based) ──────────────────────────────────────

    @Query("""
        SELECT p FROM Plan p
        WHERE p.organization.id = :orgId
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Plan> findFirstPage(UUID orgId, Pageable pageable);

    @Query("""
        SELECT p FROM Plan p
        WHERE p.organization.id = :orgId
          AND (p.createdAt < :createdAt OR (p.createdAt = :createdAt AND p.id < :id))
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Plan> findNextPage(UUID orgId, Instant createdAt, UUID id, Pageable pageable);

    @Query("""
        SELECT p FROM Plan p
        WHERE p.organization.id = :orgId
          AND (p.name ILIKE %:query% OR CAST(p.id AS string) ILIKE %:query%)
    """)
    List<Plan> searchGlobal(UUID orgId, String query, Pageable pageable);
}
