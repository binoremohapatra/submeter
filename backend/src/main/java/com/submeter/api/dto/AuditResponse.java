package com.submeter.api.dto;

import com.submeter.entity.AuditLog;
import com.submeter.entity.enums.ActorType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AuditResponse {
    private UUID id;
    private String actorEmail;
    private String action;
    private String entityType;
    private UUID entityId;
    private Map<String, Object> diff;
    private Instant createdAt;

    public AuditResponse(UUID id, String actorEmail, String action, String entityType, UUID entityId, Map<String, Object> diff, Instant createdAt) {
        this.id = id;
        this.actorEmail = actorEmail;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.diff = diff;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public Map<String, Object> getDiff() {
        return diff;
    }

    public void setDiff(Map<String, Object> diff) {
        this.diff = diff;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static AuditResponse fromEntity(AuditLog log) {
        String actorEmail = (log.getActorType() == ActorType.USER && log.getActor() != null)
                ? log.getActor().getEmail()
                : "System";

        Map<String, Object> diff = new java.util.HashMap<>();
        if (log.getOldValue() != null) diff.put("before", log.getOldValue());
        if (log.getNewValue() != null) diff.put("after", log.getNewValue());
        
        if (log.getOldValue() == null && log.getNewValue() != null && log.getAction().name().equals("CREATE")) {
            diff = log.getNewValue();
        }

        return AuditResponse.builder()
                .id(log.getId())
                .actorEmail(actorEmail)
                .action(log.getAction().name())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .diff(diff.isEmpty() ? null : diff)
                .createdAt(log.getCreatedAt())
                .build();
    }

    // Builder
    public static AuditResponseBuilder builder() {
        return new AuditResponseBuilder();
    }

    public static class AuditResponseBuilder {
        private UUID id;
        private String actorEmail;
        private String action;
        private String entityType;
        private UUID entityId;
        private Map<String, Object> diff;
        private Instant createdAt;

        public AuditResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public AuditResponseBuilder actorEmail(String actorEmail) {
            this.actorEmail = actorEmail;
            return this;
        }

        public AuditResponseBuilder action(String action) {
            this.action = action;
            return this;
        }

        public AuditResponseBuilder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public AuditResponseBuilder entityId(UUID entityId) {
            this.entityId = entityId;
            return this;
        }

        public AuditResponseBuilder diff(Map<String, Object> diff) {
            this.diff = diff;
            return this;
        }

        public AuditResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuditResponse build() {
            return new AuditResponse(id, actorEmail, action, entityType, entityId, diff, createdAt);
        }
    }
}
