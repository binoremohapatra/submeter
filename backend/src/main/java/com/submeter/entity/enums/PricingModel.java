package com.submeter.entity.enums;

/**
 * Plan pricing model. Determines how the billing engine calculates invoice amounts.
 *
 * <ul>
 *   <li>FLAT    — fixed amount per billing period; plan.flat_amount in paisa.</li>
 *   <li>TIERED  — graduated pricing; plan_tiers applied in tier_order sequence.
 *                 Each unit is priced at the rate of the tier it falls into.</li>
 *   <li>METERED — aggregated usage events summed for the billing period,
 *                 then priced via plan_tiers (same as TIERED but driven by usage_events).</li>
 * </ul>
 */
public enum PricingModel {
    FLAT,
    TIERED,
    METERED
}
