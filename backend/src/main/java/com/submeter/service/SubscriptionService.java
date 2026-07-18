package com.submeter.service;

import com.submeter.api.dto.CursorPageResponse;
import com.submeter.api.dto.SubscriptionCreateRequest;
import com.submeter.api.dto.SubscriptionResponse;
import com.submeter.entity.Customer;
import com.submeter.entity.Organization;
import com.submeter.entity.Plan;
import com.submeter.entity.Subscription;
import com.submeter.entity.enums.BillingInterval;
import com.submeter.entity.enums.SubscriptionStatus;
import com.submeter.repository.CustomerRepository;
import com.submeter.repository.OrganizationRepository;
import com.submeter.repository.PlanRepository;
import com.submeter.repository.SubscriptionRepository;
import com.submeter.repository.UserRepository;
import com.submeter.util.CursorUtil;
import com.submeter.entity.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepo;
    private final CustomerRepository     customerRepo;
    private final PlanRepository         planRepo;
    private final OrganizationRepository orgRepo;
    private final UserRepository         userRepo;
    private final AuditService           auditService;
    private final NotificationService    notificationService;

    @Transactional(readOnly = true)
    public CursorPageResponse<SubscriptionResponse> listSubscriptions(
            UUID orgId, String cursor, int limit,
            UUID customerId, UUID planId) {

        int fetchSize = Math.min(limit, 100) + 1;
        PageRequest pageRequest = PageRequest.of(0, fetchSize);

        List<Subscription> subs;
        if (customerId != null) {
            subs = subscriptionRepo.findByCustomerId(orgId, customerId, pageRequest);
        } else if (planId != null) {
            subs = subscriptionRepo.findByPlanId(orgId, planId, pageRequest);
        } else if (cursor == null || cursor.isBlank()) {
            subs = subscriptionRepo.findFirstPage(orgId, pageRequest);
        } else {
            CursorUtil.Cursor parsed = CursorUtil.parse(cursor);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            subs = subscriptionRepo.findNextPage(orgId, parsed.createdAt(), parsed.id(), pageRequest);
        }

        boolean hasNext = subs.size() > limit;
        if (hasNext) {
            subs = subs.subList(0, limit);
        }

        String nextCursor = null;
        if (hasNext && !subs.isEmpty()) {
            Subscription last = subs.get(subs.size() - 1);
            nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getId());
        }

        List<SubscriptionResponse> dtos = subs.stream()
                .map(SubscriptionResponse::fromEntity)
                .collect(Collectors.toList());

        return new CursorPageResponse<>(dtos, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID orgId, UUID subId) {
        Subscription sub = subscriptionRepo.findByIdAndOrganizationIdWithDetails(subId, orgId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Subscription not found"));
        return SubscriptionResponse.fromEntity(sub);
    }

    @Transactional
    public SubscriptionResponse createSubscription(UUID orgId, UUID actorId, SubscriptionCreateRequest req) {
        Organization org = orgRepo.getReferenceById(orgId);

        Customer customer = customerRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(req.getCustomerId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found or not in this organization."));

        Plan plan = planRepo.findByIdAndOrganizationIdWithTiers(req.getPlanId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or not in this organization."));

        if (plan.isArchived()) {
            throw new IllegalArgumentException("Cannot subscribe to an archived plan.");
        }

        Instant now = Instant.now();
        Instant periodStart = now;
        Instant periodEnd = calculatePeriodEnd(periodStart, plan.getBillingInterval());

        SubscriptionStatus status;
        Instant trialEndsAt = null;

        if (plan.getTrialDays() > 0) {
            status = SubscriptionStatus.TRIAL;
            trialEndsAt = now.plus(plan.getTrialDays(), ChronoUnit.DAYS);
            // If in trial, the first paid period hasn't started yet.
            // But we track currentPeriod as the trial period.
            periodEnd = trialEndsAt;
        } else {
            status = SubscriptionStatus.ACTIVE;
        }

        Subscription sub = Subscription.builder()
                .organization(org)
                .customer(customer)
                .plan(plan)
                .planVersion(plan.getVersion()) // Lock in the pricing version
                .status(status)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .trialEndAt(trialEndsAt)
                .build();

        sub = subscriptionRepo.save(sub);

        auditService.record(
                org,
                userRepo.getReferenceById(actorId),
                "subscription",
                sub.getId(),
                AuditAction.CREATE,
                java.util.Map.of("state", req)
        );

        return SubscriptionResponse.fromEntity(sub);
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(UUID orgId, UUID actorId, UUID subId) {
        Subscription sub = subscriptionRepo.findByIdAndOrganizationIdWithDetails(subId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        // State Machine validation
        if (sub.getStatus() == SubscriptionStatus.CANCELED) {
            throw new IllegalArgumentException("Subscription is already canceled.");
        }
        
        // Allowed transitions: TRIAL -> CANCELED, ACTIVE -> CANCELED, PAST_DUE -> CANCELED
        
        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setCanceledAt(Instant.now());
        sub = subscriptionRepo.save(sub);

        auditService.record(
                sub.getOrganization(),
                userRepo.getReferenceById(actorId),
                "subscription",
                sub.getId(),
                AuditAction.STATUS_CHANGE,
                java.util.Map.of("from", "ACTIVE", "to", "CANCELED")
        );

        notificationService.emit(
                sub.getOrganization(),
                "subscription.canceled",
                "Subscription Canceled",
                "Subscription for " + sub.getCustomer().getName() + " has been canceled.",
                "/dashboard/subscriptions/" + sub.getId()
        );

        return SubscriptionResponse.fromEntity(sub);
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private Instant calculatePeriodEnd(Instant start, BillingInterval interval) {
        ZonedDateTime zdt = start.atZone(ZoneOffset.UTC);
        return switch (interval) {
            case MONTHLY -> zdt.plusMonths(1).toInstant();
            case ANNUAL  -> zdt.plusYears(1).toInstant();
        };
    }
}
