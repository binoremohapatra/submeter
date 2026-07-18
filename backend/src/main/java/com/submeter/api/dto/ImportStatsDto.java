package com.submeter.api.dto;

public class ImportStatsDto {
    private String dataset;
    private String status;
    private String duration;
    private int customers;
    private int subscriptions;
    private int invoices;
    private int payments;
    private int usageEvents;

    // Getters and Setters
    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getCustomers() {
        return customers;
    }

    public void setCustomers(int customers) {
        this.customers = customers;
    }

    public int getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(int subscriptions) {
        this.subscriptions = subscriptions;
    }

    public int getInvoices() {
        return invoices;
    }

    public void setInvoices(int invoices) {
        this.invoices = invoices;
    }

    public int getPayments() {
        return payments;
    }

    public void setPayments(int payments) {
        this.payments = payments;
    }

    public int getUsageEvents() {
        return usageEvents;
    }

    public void setUsageEvents(int usageEvents) {
        this.usageEvents = usageEvents;
    }

    // Builder
    public static ImportStatsDtoBuilder builder() {
        return new ImportStatsDtoBuilder();
    }

    public static class ImportStatsDtoBuilder {
        private String dataset;
        private String status;
        private String duration;
        private int customers;
        private int subscriptions;
        private int invoices;
        private int payments;
        private int usageEvents;

        public ImportStatsDtoBuilder dataset(String dataset) {
            this.dataset = dataset;
            return this;
        }

        public ImportStatsDtoBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ImportStatsDtoBuilder duration(String duration) {
            this.duration = duration;
            return this;
        }

        public ImportStatsDtoBuilder customers(int customers) {
            this.customers = customers;
            return this;
        }

        public ImportStatsDtoBuilder subscriptions(int subscriptions) {
            this.subscriptions = subscriptions;
            return this;
        }

        public ImportStatsDtoBuilder invoices(int invoices) {
            this.invoices = invoices;
            return this;
        }

        public ImportStatsDtoBuilder payments(int payments) {
            this.payments = payments;
            return this;
        }

        public ImportStatsDtoBuilder usageEvents(int usageEvents) {
            this.usageEvents = usageEvents;
            return this;
        }

        public ImportStatsDto build() {
            ImportStatsDto dto = new ImportStatsDto();
            dto.setDataset(dataset);
            dto.setStatus(status);
            dto.setDuration(duration);
            dto.setCustomers(customers);
            dto.setSubscriptions(subscriptions);
            dto.setInvoices(invoices);
            dto.setPayments(payments);
            dto.setUsageEvents(usageEvents);
            return dto;
        }
    }
}
