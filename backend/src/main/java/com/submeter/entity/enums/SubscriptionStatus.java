package com.submeter.entity.enums;

/**
 * Subscription lifecycle state machine.
 *
 * <pre>
 * Valid transitions:
 *   TRIAL      → ACTIVE    (activate: manual or nightly job on trial_end_at)
 *   TRIAL      → CANCELED  (cancel at any time)
 *   ACTIVE     → PAST_DUE  (payment failed webhook)
 *   ACTIVE     → CANCELED  (cancel at any time)
 *   PAST_DUE   → ACTIVE    (successful retry payment)
 *   PAST_DUE   → CANCELED  (retry window expired or manual cancel)
 *
 * CANCELED is terminal — no transitions out.
 * </pre>
 *
 * Invalid transitions return HTTP 422 with an explicit reason message.
 * Transition logic lives in SubscriptionStateMachine (Milestone 3).
 */
public enum SubscriptionStatus {
    TRIAL,
    ACTIVE,
    PAST_DUE,
    CANCELED
}
