package com.submeter.service;

import com.submeter.api.dto.CursorPageResponse;
import com.submeter.api.dto.PlanCreateRequest;
import com.submeter.api.dto.PlanResponse;
import com.submeter.entity.Organization;
import com.submeter.entity.Plan;
import com.submeter.entity.PlanTier;
import com.submeter.entity.enums.PricingModel;
import com.submeter.repository.OrganizationRepository;
import com.submeter.repository.PlanRepository;
import com.submeter.repository.UserRepository;
import com.submeter.util.CursorUtil;
import com.submeter.entity.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepo;
    private final OrganizationRepository orgRepo;
    private final UserRepository userRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public CursorPageResponse<PlanResponse> listPlans(UUID orgId, String cursor, int limit, String search) {
        int fetchSize = Math.min(limit, 100) + 1;
        PageRequest pageRequest = PageRequest.of(0, fetchSize);

        List<Plan> plans;
        if (cursor == null || cursor.isBlank()) {
            plans = planRepo.findFirstPage(orgId, pageRequest);
        } else {
            CursorUtil.Cursor parsed = CursorUtil.parse(cursor);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            plans = planRepo.findNextPage(orgId, parsed.createdAt(), parsed.id(), pageRequest);
        }

        boolean hasNext = plans.size() > limit;
        if (hasNext) {
            plans = plans.subList(0, limit);
        }

        String nextCursor = null;
        if (hasNext && !plans.isEmpty()) {
            Plan last = plans.get(plans.size() - 1);
            nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getId());
        }

        List<PlanResponse> dtos = plans.stream()
                .map(PlanResponse::fromEntity)
                .collect(Collectors.toList());

        return new CursorPageResponse<>(dtos, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlan(UUID orgId, UUID planId) {
        Plan plan = planRepo.findByIdAndOrganizationIdWithTiers(planId, orgId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found"));
        return PlanResponse.fromEntity(plan);
    }

    @Transactional
    public PlanResponse archivePlan(UUID orgId, UUID actorId, UUID planId) {
        Plan plan = planRepo.findByIdAndOrganizationIdWithTiers(planId, orgId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Plan not found"));
        if (plan.isArchived()) {
            throw new IllegalArgumentException("Plan is already archived.");
        }
        plan.setArchived(true);
        plan = planRepo.save(plan);
        auditService.record(
                plan.getOrganization(),
                userRepo.getReferenceById(actorId),
                "plan",
                plan.getId(),
                AuditAction.STATUS_CHANGE,
                java.util.Map.of("to", "ARCHIVED")
        );
        return PlanResponse.fromEntity(plan);
    }

    @Transactional
    public PlanResponse createPlan(UUID orgId, UUID actorId, PlanCreateRequest req) {
        validatePricingModel(req);

        Organization org = orgRepo.getReferenceById(orgId);

        Plan plan = Plan.builder()
                .organization(org)
                .name(req.getName())
                .description(req.getDescription())
                .pricingModel(req.getPricingModel())
                .billingInterval(req.getBillingInterval())
                .flatAmount(req.getFlatAmount())
                .trialDays(req.getTrialDays() != null ? req.getTrialDays() : 0)
                .version(1)
                .archived(false)
                .tiers(new ArrayList<>())
                .build();

        if (req.getPricingModel() != PricingModel.FLAT && req.getTiers() != null) {
            for (int i = 0; i < req.getTiers().size(); i++) {
                var dto = req.getTiers().get(i);
                plan.getTiers().add(PlanTier.builder()
                        .plan(plan)
                        .tierOrder(i)
                        .upTo(dto.getUpTo())
                        .flatFee(dto.getFlatFee())
                        .unitAmount(dto.getUnitAmount())
                        .build());
            }
        }

        plan = planRepo.save(plan);

        auditService.record(
                org,
                userRepo.getReferenceById(actorId),
                "plan",
                plan.getId(),
                AuditAction.CREATE,
                java.util.Map.of("state", req)
        );

        return PlanResponse.fromEntity(plan);
    }

    private void validatePricingModel(PlanCreateRequest req) {
        if (req.getPricingModel() == PricingModel.FLAT) {
            if (req.getFlatAmount() == null) {
                throw new IllegalArgumentException("flatAmount is required for FLAT pricing model");
            }
        } else {
            if (req.getTiers() == null || req.getTiers().isEmpty()) {
                throw new IllegalArgumentException("Tiers are required for TIERED/METERED pricing models");
            }
            // Check that only the last tier has a null 'upTo'
            for (int i = 0; i < req.getTiers().size(); i++) {
                boolean isLast = (i == req.getTiers().size() - 1);
                Long upTo = req.getTiers().get(i).getUpTo();
                if (isLast) {
                    if (upTo != null) {
                        throw new IllegalArgumentException("The last tier must have upTo = null (infinity)");
                    }
                } else {
                    if (upTo == null) {
                        throw new IllegalArgumentException("Only the last tier can have upTo = null");
                    }
                }
            }
        }
    }
}
