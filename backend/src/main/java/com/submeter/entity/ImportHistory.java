package com.submeter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_history")
@NoArgsConstructor
@AllArgsConstructor
public class ImportHistory {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String dataset;

    @Column(nullable = false)
    private String mode;

    @Column(nullable = false)
    private String status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "customers")
    private Integer customers;

    @Column(name = "subscriptions")
    private Integer subscriptions;

    @Column(name = "invoices")
    private Integer invoices;

    @Column(name = "payments")
    private Integer payments;

    @Column(name = "usage_events")
    private Integer usageEvents;

    @Column(name = "batch_count")
    private Integer batchCount;

    @Column(name = "imported_by")
    private String importedBy;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getCustomers() {
        return customers;
    }

    public void setCustomers(Integer customers) {
        this.customers = customers;
    }

    public Integer getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Integer subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Integer getInvoices() {
        return invoices;
    }

    public void setInvoices(Integer invoices) {
        this.invoices = invoices;
    }

    public Integer getPayments() {
        return payments;
    }

    public void setPayments(Integer payments) {
        this.payments = payments;
    }

    public Integer getUsageEvents() {
        return usageEvents;
    }

    public void setUsageEvents(Integer usageEvents) {
        this.usageEvents = usageEvents;
    }

    public Integer getBatchCount() {
        return batchCount;
    }

    public void setBatchCount(Integer batchCount) {
        this.batchCount = batchCount;
    }

    public String getImportedBy() {
        return importedBy;
    }

    public void setImportedBy(String importedBy) {
        this.importedBy = importedBy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // Builder
    public static ImportHistoryBuilder builder() {
        return new ImportHistoryBuilder();
    }

    public static class ImportHistoryBuilder {
        private UUID id = UUID.randomUUID();
        private String dataset;
        private String mode;
        private String status;
        private Long durationMs;
        private Integer customers;
        private Integer subscriptions;
        private Integer invoices;
        private Integer payments;
        private Integer usageEvents;
        private Integer batchCount;
        private String importedBy;
        private String errorMessage;
        private Instant startedAt;
        private Instant completedAt;
        private Instant createdAt = Instant.now();

        public ImportHistoryBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ImportHistoryBuilder dataset(String dataset) {
            this.dataset = dataset;
            return this;
        }

        public ImportHistoryBuilder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public ImportHistoryBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ImportHistoryBuilder durationMs(Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public ImportHistoryBuilder customers(Integer customers) {
            this.customers = customers;
            return this;
        }

        public ImportHistoryBuilder subscriptions(Integer subscriptions) {
            this.subscriptions = subscriptions;
            return this;
        }

        public ImportHistoryBuilder invoices(Integer invoices) {
            this.invoices = invoices;
            return this;
        }

        public ImportHistoryBuilder payments(Integer payments) {
            this.payments = payments;
            return this;
        }

        public ImportHistoryBuilder usageEvents(Integer usageEvents) {
            this.usageEvents = usageEvents;
            return this;
        }

        public ImportHistoryBuilder batchCount(Integer batchCount) {
            this.batchCount = batchCount;
            return this;
        }

        public ImportHistoryBuilder importedBy(String importedBy) {
            this.importedBy = importedBy;
            return this;
        }

        public ImportHistoryBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ImportHistoryBuilder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public ImportHistoryBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public ImportHistoryBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ImportHistory build() {
            ImportHistory history = new ImportHistory();
            history.setId(id);
            history.setDataset(dataset);
            history.setMode(mode);
            history.setStatus(status);
            history.setDurationMs(durationMs);
            history.setCustomers(customers);
            history.setSubscriptions(subscriptions);
            history.setInvoices(invoices);
            history.setPayments(payments);
            history.setUsageEvents(usageEvents);
            history.setBatchCount(batchCount);
            history.setImportedBy(importedBy);
            history.setErrorMessage(errorMessage);
            history.setStartedAt(startedAt);
            history.setCompletedAt(completedAt);
            history.setCreatedAt(createdAt);
            return history;
        }
    }
}
