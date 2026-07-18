package com.submeter.repository;

import com.submeter.entity.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {

    /**
     * Used by the NightlyBillingJob to aggregate total usage for a subscription
     * within a specific billing period.
     */
    @Query("""
        SELECT COALESCE(SUM(u.quantity), 0)
        FROM UsageEvent u
        WHERE u.subscription.id = :subscriptionId
          AND u.occurredAt >= :periodStart
          AND u.occurredAt < :periodEnd
    """)
    long sumUsageForSubscriptionInPeriod(UUID subscriptionId, Instant periodStart, Instant periodEnd);
}
