package com.submeter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private String type;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String body;

    @Column(updatable = false)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "visible_to_role", updatable = false)
    private String visibleToRole;

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getVisibleToRole() {
        return visibleToRole;
    }

    public void setVisibleToRole(String visibleToRole) {
        this.visibleToRole = visibleToRole;
    }

    // Builder
    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }

    public static class NotificationBuilder {
        private Organization organization;
        private User user;
        private String type;
        private String title;
        private String body;
        private String link;
        private Instant readAt;
        private String visibleToRole;

        public NotificationBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public NotificationBuilder user(User user) {
            this.user = user;
            return this;
        }

        public NotificationBuilder type(String type) {
            this.type = type;
            return this;
        }

        public NotificationBuilder title(String title) {
            this.title = title;
            return this;
        }

        public NotificationBuilder body(String body) {
            this.body = body;
            return this;
        }

        public NotificationBuilder link(String link) {
            this.link = link;
            return this;
        }

        public NotificationBuilder readAt(Instant readAt) {
            this.readAt = readAt;
            return this;
        }

        public NotificationBuilder visibleToRole(String visibleToRole) {
            this.visibleToRole = visibleToRole;
            return this;
        }

        public Notification build() {
            Notification notification = new Notification();
            notification.setOrganization(organization);
            notification.setUser(user);
            notification.setType(type);
            notification.setTitle(title);
            notification.setBody(body);
            notification.setLink(link);
            notification.setReadAt(readAt);
            notification.setVisibleToRole(visibleToRole);
            return notification;
        }
    }
}
