package com.submeter.service;

import com.submeter.api.dto.UsageEventCreateRequest;
import com.submeter.entity.Subscription;
import com.submeter.entity.UsageEvent;
import com.submeter.entity.enums.SubscriptionStatus;
import com.submeter.repository.SubscriptionRepository;
import com.submeter.repository.UsageEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsageEventService {

    private static final Logger log = LoggerFactory.getLogger(UsageEventService.class);

    private final UsageEventRepository   usageRepo;
    private final SubscriptionRepository subscriptionRepo;

    /**
     * Ingests a single usage event.
     * Enforces idempotency via DB UNIQUE constraint on (organization_id, idempotency_key).
     */
    @Transactional
    public void ingestEvent(UUID orgId, UsageEventCreateRequest req) {
        Subscription sub = subscriptionRepo.findByIdAndOrganizationIdWithDetails(req.getSubscriptionId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (sub.getStatus() != SubscriptionStatus.ACTIVE && sub.getStatus() != SubscriptionStatus.TRIAL) {
            throw new IllegalArgumentException("Cannot report usage for a non-active subscription.");
        }

        UsageEvent event = UsageEvent.builder()
                .organization(sub.getOrganization())
                .subscription(sub)
                .idempotencyKey(req.getIdempotencyKey())
                .eventType(req.getEventType())
                .quantity(req.getQuantity())
                .occurredAt(req.getOccurredAt())
                .build();

        try {
            usageRepo.saveAndFlush(event);
        } catch (DataIntegrityViolationException e) {
            // DB constraint: unique index on (org_id, idempotency_key)
            log.warn("Ignored duplicate usage event orgId={} idempotencyKey={}", 
                     orgId, req.getIdempotencyKey());
            // Idempotent operation: simply return success to the client instead of erroring
        }
    }
}
