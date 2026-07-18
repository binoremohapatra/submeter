package com.submeter.api.dto;

import com.submeter.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public class NotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String body;
    private String link;
    private Instant readAt;
    private Instant createdAt;

    public NotificationResponse(UUID id, String type, String title, String body, String link, Instant readAt, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static NotificationResponse fromEntity(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .body(entity.getBody())
                .link(entity.getLink())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // Builder
    public static NotificationResponseBuilder builder() {
        return new NotificationResponseBuilder();
    }

    public static class NotificationResponseBuilder {
        private UUID id;
        private String type;
        private String title;
        private String body;
        private String link;
        private Instant readAt;
        private Instant createdAt;

        public NotificationResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public NotificationResponseBuilder type(String type) {
            this.type = type;
            return this;
        }

        public NotificationResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public NotificationResponseBuilder body(String body) {
            this.body = body;
            return this;
        }

        public NotificationResponseBuilder link(String link) {
            this.link = link;
            return this;
        }

        public NotificationResponseBuilder readAt(Instant readAt) {
            this.readAt = readAt;
            return this;
        }

        public NotificationResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NotificationResponse build() {
            return new NotificationResponse(id, type, title, body, link, readAt, createdAt);
        }
    }
}
