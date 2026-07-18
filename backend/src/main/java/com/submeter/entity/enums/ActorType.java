package com.submeter.entity.enums;

/**
 * Who performed the audited action.
 * <ul>
 *   <li>USER   — a logged-in user; actor_id will be non-null.</li>
 *   <li>SYSTEM — an internal job (e.g. NightlyBillingJob); actor_id will be null.</li>
 * </ul>
 */
public enum ActorType {
    USER,
    SYSTEM
}
