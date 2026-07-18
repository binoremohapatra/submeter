package com.submeter.repository;

import com.submeter.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Used at signup to check slug uniqueness.
     * All active orgs only (soft-delete excluded).
     */
    Optional<Organization> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndDeletedAtIsNull(String slug);
}
