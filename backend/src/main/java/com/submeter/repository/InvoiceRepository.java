package com.submeter.repository;

import com.submeter.entity.Invoice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Query(value = "SELECT next_invoice_number(CAST(:orgId AS UUID))", nativeQuery = true)
    String generateInvoiceNumber(UUID orgId);

    @Query("""
        SELECT i FROM Invoice i
        JOIN FETCH i.subscription s
        JOIN FETCH s.customer c
        LEFT JOIN FETCH i.lineItems
        WHERE i.id = :id AND i.organization.id = :orgId
    """)
    Optional<Invoice> findByIdAndOrganizationIdWithLineItems(UUID id, UUID orgId);

    // ── Keyset Pagination (Cursor based) ──────────────────────────────────────

    @Query("""
        SELECT i FROM Invoice i
        JOIN FETCH i.subscription s
        WHERE i.organization.id = :orgId
        ORDER BY i.createdAt DESC, i.id DESC
    """)
    List<Invoice> findFirstPage(UUID orgId, Pageable pageable);

    @Query("""
        SELECT i FROM Invoice i
        JOIN FETCH i.subscription s
        WHERE i.organization.id = :orgId
          AND (i.createdAt < :createdAt OR (i.createdAt = :createdAt AND i.id < :id))
        ORDER BY i.createdAt DESC, i.id DESC
    """)
    List<Invoice> findNextPage(UUID orgId, Instant createdAt, UUID id, Pageable pageable);

    // ── Filtered by customer ───────────────────────────────────────────────────

    @Query("""
        SELECT i FROM Invoice i
        JOIN FETCH i.subscription s
        WHERE i.organization.id = :orgId
          AND s.customer.id = :customerId
        ORDER BY i.createdAt DESC, i.id DESC
    """)
    List<Invoice> findByCustomerId(UUID orgId, UUID customerId, Pageable pageable);

    // ── Filtered by subscription ───────────────────────────────────────────────

    @Query("""
        SELECT i FROM Invoice i
        JOIN FETCH i.subscription s
        WHERE i.organization.id = :orgId
          AND i.subscription.id = :subscriptionId
        ORDER BY i.createdAt DESC, i.id DESC
    """)
    List<Invoice> findBySubscriptionId(UUID orgId, UUID subscriptionId, Pageable pageable);

    @Query("""
        SELECT i FROM Invoice i
        JOIN FETCH i.subscription s
        WHERE i.organization.id = :orgId
          AND (i.invoiceNumber ILIKE %:query% OR CAST(i.id AS string) ILIKE %:query%)
    """)
    List<Invoice> searchGlobal(UUID orgId, String query, Pageable pageable);
}
