package com.submeter.api;

import com.submeter.api.dto.UsageEventCreateRequest;
import com.submeter.entity.enums.Role;
import com.submeter.security.UserPrincipal;
import com.submeter.security.rbac.RequiresRole;
import com.submeter.service.UsageEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usage")
@RequiredArgsConstructor
public class UsageEventController {

    private final UsageEventService usageService;

    @PostMapping
    @RequiresRole(minimum = Role.ADMIN) // Requires ADMIN because it affects billing
    public ResponseEntity<Void> ingestUsage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UsageEventCreateRequest req
    ) {
        usageService.ingestEvent(principal.getOrgId(), req);
        return ResponseEntity.accepted().build();
    }
}
