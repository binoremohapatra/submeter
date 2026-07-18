package com.submeter.service;

import com.submeter.entity.AuditLog;
import com.submeter.entity.Organization;
import com.submeter.entity.User;
import com.submeter.entity.enums.ActorType;
import com.submeter.entity.enums.AuditAction;
import com.submeter.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Record an audit event.
     * Propagation.MANDATORY ensures that this is only called within an existing transaction.
     * If the transaction rolls back, the audit log will also roll back, which is correct
     * because the mutation didn't actually happen.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Organization org, User actor, String entityType, UUID entityId, AuditAction action, Map<String, Object> diff) {
        Map<String, Object> oldValue = null;
        Map<String, Object> newValue = null;

        if (diff != null) {
            if (action == AuditAction.UPDATE && (diff.containsKey("before") || diff.containsKey("after"))) {
                oldValue = (Map<String, Object>) diff.get("before");
                newValue = (Map<String, Object>) diff.get("after");
            } else if (action == AuditAction.CREATE) {
                newValue = diff;
            }
        }

        recordExtended(org, actor, entityType, entityId, action, oldValue, newValue, entityType, entityId.toString(), true, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordExtended(Organization org, User actor, String entityType, UUID entityId, AuditAction action, 
                               Map<String, Object> oldValue, Map<String, Object> newValue,
                               String resourceType, String resourceName, Boolean success, Long durationMs) {
        String ipAddress = null;
        String userAgent = null;
        String requestId = null;
        String correlationId = null;

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            ipAddress = attributes.getRequest().getRemoteAddr();
            userAgent = attributes.getRequest().getHeader("User-Agent");
            requestId = attributes.getRequest().getHeader("X-Request-ID");
            correlationId = attributes.getRequest().getHeader("X-Correlation-ID");
        }

        AuditLog log = AuditLog.builder()
                .organization(org)
                .actor(actor)
                .actorType(actor != null ? ActorType.USER : ActorType.SYSTEM)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestId(requestId)
                .correlationId(correlationId)
                .resourceType(resourceType)
                .resourceName(resourceName)
                .success(success)
                .durationMs(durationMs)
                .build();

        auditLogRepository.save(log);
    }
    @Transactional(readOnly = true)
    public com.submeter.api.dto.CursorPageResponse<com.submeter.api.dto.AuditResponse> listAuditLogs(UUID orgId, String entityType, String cursor, int limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit + 1);

        java.util.List<AuditLog> logs;
        if (cursor == null || cursor.isBlank()) {
            logs = auditLogRepository.findFirstPage(orgId, entityType, pageable);
        } else {
            com.submeter.util.CursorUtil.Cursor c = com.submeter.util.CursorUtil.parse(cursor);
            logs = auditLogRepository.findNextPage(orgId, entityType, c.createdAt(), c.id(), pageable);
        }

        boolean hasNext = logs.size() > limit;
        if (hasNext) {
            logs = logs.subList(0, limit);
        }

        String nextCursor = null;
        if (hasNext && !logs.isEmpty()) {
            AuditLog last = logs.get(logs.size() - 1);
            nextCursor = com.submeter.util.CursorUtil.encode(last.getCreatedAt(), last.getId());
        }

        java.util.List<com.submeter.api.dto.AuditResponse> dtos = logs.stream()
                .map(com.submeter.api.dto.AuditResponse::fromEntity)
                .toList();

        return new com.submeter.api.dto.CursorPageResponse<>(dtos, nextCursor, hasNext);
    }
}
