package com.submeter.repository;

import com.submeter.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find active (non-revoked, non-expired) refresh token by its opaque value.
     * Used during token rotation and validation.
     */
    @Query("""
        SELECT rt FROM RefreshToken rt
        WHERE rt.token = :token
          AND rt.revokedAt IS NULL
          AND rt.expiresAt > :now
    """)
    Optional<RefreshToken> findActiveByToken(String token, Instant now);

    /**
     * Revoke all existing refresh tokens for a user (called on login to enforce single-session).
     * In v1 we allow only one active session per user.
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revokedAt = :now
        WHERE rt.user.id = :userId
          AND rt.revokedAt IS NULL
    """)
    void revokeAllForUser(UUID userId, Instant now);

    /** Delete expired + revoked tokens older than retention window (called by cleanup job). */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :before OR rt.revokedAt < :before")
    void deleteExpiredBefore(Instant before);
}
