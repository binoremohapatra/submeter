package com.submeter.api.dto;

import com.submeter.entity.Customer;
import com.submeter.entity.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class CustomerResponse {
    private final UUID id;
    private final String name;
    private final String email;
    private final long activeSubscriptions;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CustomerResponse(UUID id, String name, String email, long activeSubscriptions, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.activeSubscriptions = activeSubscriptions;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CustomerResponse fromEntity(Customer customer) {
        long activeSubs = 0;
        if (customer.getSubscriptions() != null) {
            activeSubs = customer.getSubscriptions().stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                              || s.getStatus() == SubscriptionStatus.TRIAL)
                    .count();
        }
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .activeSubscriptions(activeSubs)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    // Builder
    public static CustomerResponseBuilder builder() {
        return new CustomerResponseBuilder();
    }

    public static class CustomerResponseBuilder {
        private UUID id;
        private String name;
        private String email;
        private long activeSubscriptions;
        private Instant createdAt;
        private Instant updatedAt;

        public CustomerResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public CustomerResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CustomerResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public CustomerResponseBuilder activeSubscriptions(long activeSubscriptions) {
            this.activeSubscriptions = activeSubscriptions;
            return this;
        }

        public CustomerResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CustomerResponseBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public CustomerResponse build() {
            return new CustomerResponse(id, name, email, activeSubscriptions, createdAt, updatedAt);
        }
    }
}
