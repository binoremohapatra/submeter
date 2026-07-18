package com.submeter.api;

import com.submeter.api.dto.CursorPageResponse;
import com.submeter.api.dto.SubscriptionCreateRequest;
import com.submeter.api.dto.SubscriptionResponse;
import com.submeter.entity.enums.Role;
import com.submeter.security.UserPrincipal;
import com.submeter.security.rbac.RequiresRole;
import com.submeter.service.SubscriptionService;
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
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<CursorPageResponse<SubscriptionResponse>> listSubscriptions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID planId
    ) {
        int safeLimit = Math.min(limit, 100);
        return ResponseEntity.ok(
                subscriptionService.listSubscriptions(principal.getOrgId(), cursor, safeLimit, customerId, planId));
    }

    @GetMapping("/{id}")
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.getSubscription(principal.getOrgId(), id));
    }

    @PostMapping
    @RequiresRole(minimum = Role.ADMIN)
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubscriptionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.createSubscription(principal.getOrgId(), principal.getUserId(), req));
    }

    @PostMapping("/{id}/cancel")
    @RequiresRole(minimum = Role.ADMIN)
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(principal.getOrgId(), principal.getUserId(), id));
    }
}
