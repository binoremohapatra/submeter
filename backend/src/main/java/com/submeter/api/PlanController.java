package com.submeter.api;

import com.submeter.api.dto.CursorPageResponse;
import com.submeter.api.dto.PlanCreateRequest;
import com.submeter.api.dto.PlanResponse;
import com.submeter.entity.enums.Role;
import com.submeter.security.UserPrincipal;
import com.submeter.security.rbac.RequiresRole;
import com.submeter.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<CursorPageResponse<PlanResponse>> listPlans(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search
    ) {
        int safeLimit = Math.min(limit, 100);
        return ResponseEntity.ok(planService.listPlans(principal.getOrgId(), cursor, safeLimit, search));
    }

    @GetMapping("/{id}")
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<PlanResponse> getPlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(planService.getPlan(principal.getOrgId(), id));
    }

    @PostMapping("/{id}/archive")
    @RequiresRole(minimum = Role.ADMIN)
    public ResponseEntity<PlanResponse> archivePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(planService.archivePlan(principal.getOrgId(), principal.getUserId(), id));
    }

    @PostMapping
    @RequiresRole(minimum = Role.ADMIN)
    public ResponseEntity<PlanResponse> createPlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PlanCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.createPlan(principal.getOrgId(), principal.getUserId(), req));
    }
}
