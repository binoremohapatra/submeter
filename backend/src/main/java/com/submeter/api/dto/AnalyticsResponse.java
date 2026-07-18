package com.submeter.api.dto;

import lombok.Builder;
import lombok.Getter;

public class AnalyticsResponse {
    private final long mrrCents;
    private final long totalActiveSubscriptions;
    private final long arpuCents;
    private final double churnRate;

    public AnalyticsResponse(long mrrCents, long totalActiveSubscriptions, long arpuCents, double churnRate) {
        this.mrrCents = mrrCents;
        this.totalActiveSubscriptions = totalActiveSubscriptions;
        this.arpuCents = arpuCents;
        this.churnRate = churnRate;
    }

    // Getters
    public long getMrrCents() {
        return mrrCents;
    }

    public long getTotalActiveSubscriptions() {
        return totalActiveSubscriptions;
    }

    public long getArpuCents() {
        return arpuCents;
    }

    public double getChurnRate() {
        return churnRate;
    }

    // Builder
    public static AnalyticsResponseBuilder builder() {
        return new AnalyticsResponseBuilder();
    }

    public static class AnalyticsResponseBuilder {
        private long mrrCents;
        private long totalActiveSubscriptions;
        private long arpuCents;
        private double churnRate;

        public AnalyticsResponseBuilder mrrCents(long mrrCents) {
            this.mrrCents = mrrCents;
            return this;
        }

        public AnalyticsResponseBuilder totalActiveSubscriptions(long totalActiveSubscriptions) {
            this.totalActiveSubscriptions = totalActiveSubscriptions;
            return this;
        }

        public AnalyticsResponseBuilder arpuCents(long arpuCents) {
            this.arpuCents = arpuCents;
            return this;
        }

        public AnalyticsResponseBuilder churnRate(double churnRate) {
            this.churnRate = churnRate;
            return this;
        }

        public AnalyticsResponse build() {
            return new AnalyticsResponse(mrrCents, totalActiveSubscriptions, arpuCents, churnRate);
        }
    }
}
