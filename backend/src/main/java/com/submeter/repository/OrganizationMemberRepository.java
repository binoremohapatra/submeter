package com.submeter.repository;

import com.submeter.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    Optional<OrganizationMember> findByOrganizationIdAndId(UUID orgId, UUID id);
    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID orgId, UUID userId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    List<OrganizationMember> findAllByOrganizationId(UUID orgId);
}
