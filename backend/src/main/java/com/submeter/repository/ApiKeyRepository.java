package com.submeter.repository;

import com.submeter.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByOrganizationIdAndId(UUID orgId, UUID id);
    Optional<ApiKey> findByOrganizationIdAndKeyId(UUID orgId, String keyId);
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findAllByOrganizationId(UUID orgId);
}
