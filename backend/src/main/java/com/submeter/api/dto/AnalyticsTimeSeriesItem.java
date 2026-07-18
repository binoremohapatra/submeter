package com.submeter.api.dto;

import lombok.Builder;
import lombok.Data;

public class AnalyticsTimeSeriesItem {
    private String date; // "MMM d" format, e.g. "Oct 12"
    private long mrrCents;

    public AnalyticsTimeSeriesItem(String date, long mrrCents) {
        this.date = date;
        this.mrrCents = mrrCents;
    }

    // Getters and Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getMrrCents() {
        return mrrCents;
    }

    public void setMrrCents(long mrrCents) {
        this.mrrCents = mrrCents;
    }

    // Builder
    public static AnalyticsTimeSeriesItemBuilder builder() {
        return new AnalyticsTimeSeriesItemBuilder();
    }

    public static class AnalyticsTimeSeriesItemBuilder {
        private String date;
        private long mrrCents;

        public AnalyticsTimeSeriesItemBuilder date(String date) {
            this.date = date;
            return this;
        }

        public AnalyticsTimeSeriesItemBuilder mrrCents(long mrrCents) {
            this.mrrCents = mrrCents;
            return this;
        }

        public AnalyticsTimeSeriesItem build() {
            return new AnalyticsTimeSeriesItem(date, mrrCents);
        }
    }
}
