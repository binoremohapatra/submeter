package com.submeter.repository;

import com.submeter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Primary lookup for auth — finds non-deleted user by email.
     * Email is globally unique (DB UNIQUE constraint on users.email).
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    /**
     * Used by RBAC interceptor to re-verify role from DB.
     * Lightweight — only loads id + role, not the full entity.
     * Returns empty if user is soft-deleted (deleted_at IS NOT NULL).
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.id = :userId
          AND u.organization.id = :orgId
          AND u.deletedAt IS NULL
    """)
    Optional<User> findActiveByIdAndOrgId(UUID userId, UUID orgId);

    List<User> findAllByOrganizationIdAndDeletedAtIsNull(UUID orgId);

    /** Rate-limiter reset: clear failed attempts on successful login. */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginCount = 0, u.lockedUntil = NULL WHERE u.id = :userId")
    void resetFailedAttempts(UUID userId);

    /** Increment failed attempts + set lock if threshold reached. */
    @Modifying
    @Query("""
        UPDATE User u
        SET u.failedLoginCount = u.failedLoginCount + 1,
            u.lockedUntil = :lockUntil
        WHERE u.id = :userId
    """)
    void incrementFailedAttempts(UUID userId, Instant lockUntil);

    /** Update last login timestamp. */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :now WHERE u.id = :userId")
    void updateLastLogin(UUID userId, Instant now);
}
