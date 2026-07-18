package com.submeter.service;

import com.submeter.api.dto.AnalyticsResponse;
import com.submeter.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SubscriptionRepository subscriptionRepo;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(UUID orgId) {
        long mrrCents = subscriptionRepo.calculateMrrFlatCents(orgId);
        long activeSubs = subscriptionRepo.countActiveSubscriptions(orgId);
        
        long arpuCents = 0L;
        if (activeSubs > 0) {
            arpuCents = mrrCents / activeSubs;
        }

        // Mock churn for dashboard demonstration purposes (a real implementation 
        // would compare canceled_at within 30 days over total active 30 days ago).
        double churnRate = 0.025; // 2.5%

        return AnalyticsResponse.builder()
                .mrrCents(mrrCents)
                .totalActiveSubscriptions(activeSubs)
                .arpuCents(arpuCents)
                .churnRate(churnRate)
                .build();
    }
    @Transactional(readOnly = true)
    public java.util.List<com.submeter.api.dto.AnalyticsTimeSeriesItem> getHistoricalMrr(UUID orgId, int days) {
        java.util.List<com.submeter.entity.Subscription> subs = subscriptionRepo.findFlatSubscriptions(orgId);
        java.util.List<com.submeter.api.dto.AnalyticsTimeSeriesItem> series = new java.util.ArrayList<>();
        
        java.time.Instant now = java.time.Instant.now();
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Kolkata");
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d").withZone(zone);

        for (int i = days; i >= 0; i--) {
            java.time.Instant dayStart = now.minus(i, java.time.temporal.ChronoUnit.DAYS);
            long dailyMrr = 0;
            
            for (com.submeter.entity.Subscription sub : subs) {
                // Must have started before this day
                if (sub.getCurrentPeriodStart() != null && !sub.getCurrentPeriodStart().isAfter(dayStart)) {
                    // Must not be canceled before this day
                    if (sub.getCanceledAt() == null || sub.getCanceledAt().isAfter(dayStart)) {
                        long amt = sub.getPlan().getFlatAmount();
                        if (sub.getPlan().getBillingInterval() == com.submeter.entity.enums.BillingInterval.ANNUAL) {
                            amt = amt / 12;
                        }
                        dailyMrr += amt;
                    }
                }
            }
            
            series.add(com.submeter.api.dto.AnalyticsTimeSeriesItem.builder()
                    .date(formatter.format(dayStart))
                    .mrrCents(dailyMrr)
                    .build());
        }
        
        return series;
    }
}
