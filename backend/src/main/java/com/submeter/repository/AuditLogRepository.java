package com.submeter.repository;

import com.submeter.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.organization.id = :orgId
          AND (:entityType IS NULL OR a.entityType = :entityType)
        ORDER BY a.createdAt DESC, a.id DESC
    """)
    List<AuditLog> findFirstPage(UUID orgId, String entityType, Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.organization.id = :orgId
          AND (:entityType IS NULL OR a.entityType = :entityType)
          AND (a.createdAt < :createdAt OR (a.createdAt = :createdAt AND a.id < :id))
        ORDER BY a.createdAt DESC, a.id DESC
    """)
    List<AuditLog> findNextPage(UUID orgId, String entityType, Instant createdAt, UUID id, Pageable pageable);
}
