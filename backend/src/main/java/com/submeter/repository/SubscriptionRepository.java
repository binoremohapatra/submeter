package com.submeter.repository;

import com.submeter.entity.Subscription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.customer c
        JOIN FETCH s.plan p
        WHERE s.id = :id AND s.organization.id = :orgId
    """)
    Optional<Subscription> findByIdAndOrganizationIdWithDetails(UUID id, UUID orgId);

    // ── Keyset Pagination (Cursor based) ──────────────────────────────────────

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.customer c
        JOIN FETCH s.plan p
        WHERE s.organization.id = :orgId
        ORDER BY s.createdAt DESC, s.id DESC
    """)
    List<Subscription> findFirstPage(UUID orgId, Pageable pageable);

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.customer c
        JOIN FETCH s.plan p
        WHERE s.organization.id = :orgId
          AND (s.createdAt < :createdAt OR (s.createdAt = :createdAt AND s.id < :id))
        ORDER BY s.createdAt DESC, s.id DESC
    """)
    List<Subscription> findNextPage(UUID orgId, Instant createdAt, UUID id, Pageable pageable);

    // ── Filtered by customer ───────────────────────────────────────────────────

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.customer c
        JOIN FETCH s.plan p
        WHERE s.organization.id = :orgId
          AND s.customer.id = :customerId
        ORDER BY s.createdAt DESC, s.id DESC
    """)
    List<Subscription> findByCustomerId(UUID orgId, UUID customerId, Pageable pageable);

    // ── Filtered by plan ───────────────────────────────────────────────────────

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.customer c
        JOIN FETCH s.plan p
        WHERE s.organization.id = :orgId
          AND s.plan.id = :planId
        ORDER BY s.createdAt DESC, s.id DESC
    """)
    List<Subscription> findByPlanId(UUID orgId, UUID planId, Pageable pageable);

    // ── Billing Job Queries ───────────────────────────────────────────────────

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.plan p
        WHERE s.status = 'TRIAL'
          AND s.trialEndAt <= :now
    """)
    List<Subscription> findTrialsExpiringBefore(Instant now);

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.customer c
        JOIN FETCH s.plan p
        LEFT JOIN FETCH p.tiers
        WHERE s.status = 'ACTIVE'
          AND s.currentPeriodEnd <= :now
    """)
    List<Subscription> findActiveSubscriptionsEndingBefore(Instant now);

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN p.billingInterval = 'MONTHLY' THEN p.flatAmount
                WHEN p.billingInterval = 'ANNUAL' THEN p.flatAmount / 12
                ELSE 0
            END
        ), 0)
        FROM Subscription s
        JOIN s.plan p
        WHERE s.organization.id = :orgId
          AND s.status IN ('ACTIVE', 'PAST_DUE')
          AND p.pricingModel = 'FLAT'
    """)
    long calculateMrrFlatCents(UUID orgId);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.organization.id = :orgId AND s.status IN ('ACTIVE', 'PAST_DUE', 'TRIAL')")
    long countActiveSubscriptions(UUID orgId);

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.plan p
        WHERE s.organization.id = :orgId
          AND p.pricingModel = 'FLAT'
    """)
    List<Subscription> findFlatSubscriptions(UUID orgId);

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.plan p
        JOIN FETCH s.customer c
        WHERE s.organization.id = :orgId
          AND (CAST(s.id AS string) ILIKE %:query% OR p.name ILIKE %:query% OR c.name ILIKE %:query% OR c.email ILIKE %:query%)
    """)
    List<Subscription> searchGlobal(UUID orgId, String query, Pageable pageable);
}
