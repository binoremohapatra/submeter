package com.submeter.api;

import com.submeter.api.dto.AnalyticsResponse;
import com.submeter.entity.enums.Role;
import com.submeter.security.UserPrincipal;
import com.submeter.security.rbac.RequiresRole;
import com.submeter.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(analyticsService.getAnalytics(principal.getOrgId()));
    }

    @GetMapping("/mrr")
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<java.util.List<com.submeter.api.dto.AnalyticsTimeSeriesItem>> getHistoricalMrr(
            @AuthenticationPrincipal UserPrincipal principal,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analyticsService.getHistoricalMrr(principal.getOrgId(), days));
    }
}
