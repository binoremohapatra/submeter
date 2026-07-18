package com.submeter.entity;

import com.submeter.entity.enums.ActorType;
import com.submeter.entity.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit log entry. One row per mutation.
 *
 * <p>Append-only contract: no UPDATE or DELETE is ever issued against this table
 * in application code. The V1 migration does not define a unique constraint that
 * would allow "editing" an entry. The {@code updated_at} column exists only for
 * {@link BaseEntity} consistency; its value will always equal {@code created_at}.
 *
 * <p>Hook points (Milestone 7): the audit log is populated by hooking into the
 * existing service-layer mutation methods — no new write paths are introduced.
 * The {@code AuditService.record()} method is called at the end of each mutation
 * after the main entity has been successfully persisted.
 *
 * <p>{@code diff} shape by action:
 * <pre>
 * CREATE:        {"state": { ...full entity snapshot... }}
 * UPDATE:        {"before": { ...old values... }, "after": { ...new values... }}
 * DELETE:        {"deleted_at": "2025-06-01T00:00:00Z"}
 * STATUS_CHANGE: {"from": "TRIAL", "to": "ACTIVE", "entity": "subscription"}
 * </pre>
 *
 * <p>FLAG: {@code entityId} NOT NULL — without it the audit log is un-queryable per entity.
 * FLAG: {@code actorType} NOT NULL — must always know if this was a user or a system action.
 */
@Entity
@Table(name = "audit_log")
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    /**
     * The user who performed the action.
     * Null when {@code actorType == SYSTEM} (e.g. nightly billing job).
     * References {@link User} — remains valid even if the user is soft-deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", updatable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10, updatable = false)
    private ActorType actorType;

    /**
     * Entity type string, e.g. {@code "customer"}, {@code "subscription"}, {@code "invoice"}.
     * Lower-cased snake_case to match table names.
     */
    @Column(name = "entity_type", nullable = false, updatable = false)
    private String entityType;

    /** PK of the affected entity. Used with {@code entity_type} to reconstruct the audit trail. */
    @Column(name = "entity_id", nullable = false, updatable = false,
            columnDefinition = "uuid")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20, updatable = false)
    private AuditAction action;

    @Column(name = "request_id", updatable = false)
    private String requestId;

    @Column(name = "correlation_id", updatable = false)
    private String correlationId;

    @Column(name = "resource_type", updatable = false)
    private String resourceType;

    @Column(name = "resource_name", updatable = false)
    private String resourceName;

    @Column(name = "success", updatable = false)
    private Boolean success;

    @Column(name = "duration_ms", updatable = false)
    private Long durationMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> newValue;

    /** Client IP at mutation time. Stored as TEXT (not INET) for portability. */
    @Column(name = "ip_address", updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false)
    private String userAgent;

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public void setActorType(ActorType actorType) {
        this.actorType = actorType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Map<String, Object> getOldValue() {
        return oldValue;
    }

    public void setOldValue(Map<String, Object> oldValue) {
        this.oldValue = oldValue;
    }

    public Map<String, Object> getNewValue() {
        return newValue;
    }

    public void setNewValue(Map<String, Object> newValue) {
        this.newValue = newValue;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    // Builder
    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }

    public static class AuditLogBuilder {
        private Organization organization;
        private User actor;
        private ActorType actorType;
        private String entityType;
        private UUID entityId;
        private AuditAction action;
        private String requestId;
        private String correlationId;
        private String resourceType;
        private String resourceName;
        private Boolean success;
        private Long durationMs;
        private Map<String, Object> oldValue;
        private Map<String, Object> newValue;
        private String ipAddress;
        private String userAgent;

        public AuditLogBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public AuditLogBuilder actor(User actor) {
            this.actor = actor;
            return this;
        }

        public AuditLogBuilder actorType(ActorType actorType) {
            this.actorType = actorType;
            return this;
        }

        public AuditLogBuilder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public AuditLogBuilder entityId(UUID entityId) {
            this.entityId = entityId;
            return this;
        }

        public AuditLogBuilder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public AuditLogBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public AuditLogBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public AuditLogBuilder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public AuditLogBuilder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public AuditLogBuilder success(Boolean success) {
            this.success = success;
            return this;
        }

        public AuditLogBuilder durationMs(Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public AuditLogBuilder oldValue(Map<String, Object> oldValue) {
            this.oldValue = oldValue;
            return this;
        }

        public AuditLogBuilder newValue(Map<String, Object> newValue) {
            this.newValue = newValue;
            return this;
        }

        public AuditLogBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public AuditLogBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public AuditLog build() {
            AuditLog auditLog = new AuditLog();
            auditLog.setOrganization(organization);
            auditLog.setActor(actor);
            auditLog.setActorType(actorType);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setRequestId(requestId);
            auditLog.setCorrelationId(correlationId);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceName(resourceName);
            auditLog.setSuccess(success);
            auditLog.setDurationMs(durationMs);
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            return auditLog;
        }
    }
}
