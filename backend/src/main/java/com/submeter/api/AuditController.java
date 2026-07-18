package com.submeter.api;

import com.submeter.api.dto.AuditResponse;
import com.submeter.api.dto.CursorPageResponse;
import com.submeter.entity.enums.Role;
import com.submeter.security.UserPrincipal;
import com.submeter.security.rbac.RequiresRole;
import com.submeter.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<CursorPageResponse<AuditResponse>> listAuditLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit
    ) {
        int safeLimit = Math.min(limit, 100);
        return ResponseEntity.ok(auditService.listAuditLogs(principal.getOrgId(), entityType, cursor, safeLimit));
    }
}
