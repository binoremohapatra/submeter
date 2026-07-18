package com.submeter.repository;

import com.submeter.entity.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID orgId);

    @Query("""
        SELECT c FROM Customer c
        LEFT JOIN FETCH c.subscriptions
        WHERE c.id = :id AND c.organization.id = :orgId AND c.deletedAt IS NULL
    """)
    Optional<Customer> findByIdWithSubscriptionsAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID orgId);

    List<Customer> findAllByOrganizationIdAndDeletedAtIsNull(UUID orgId);

    boolean existsByEmailAndOrganizationIdAndDeletedAtIsNull(String email, UUID orgId);

    // ── Keyset Pagination (Cursor based) ──────────────────────────────────────

    @Query("""
        SELECT c FROM Customer c
        WHERE c.organization.id = :orgId
          AND c.deletedAt IS NULL
        ORDER BY c.createdAt DESC, c.id DESC
    """)
    List<Customer> findFirstPage(UUID orgId, Pageable pageable);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.organization.id = :orgId
          AND c.deletedAt IS NULL
          AND (c.createdAt < :createdAt OR (c.createdAt = :createdAt AND c.id < :id))
        ORDER BY c.createdAt DESC, c.id DESC
    """)
    List<Customer> findNextPage(UUID orgId, Instant createdAt, UUID id, Pageable pageable);

    @Query("SELECT c.externalId FROM Customer c WHERE c.externalId IS NOT NULL")
    java.util.Set<String> findAllExternalIds();

    @Query("""
        SELECT c FROM Customer c
        WHERE c.organization.id = :orgId
          AND c.deletedAt IS NULL
          AND (c.name ILIKE %:query% OR c.email ILIKE %:query% OR CAST(c.id AS string) ILIKE %:query% OR c.externalId ILIKE %:query%)
    """)
    List<Customer> searchGlobal(UUID orgId, String query, Pageable pageable);
}
