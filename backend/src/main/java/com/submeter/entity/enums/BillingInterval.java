package com.submeter.entity.enums;

/**
 * Billing cycle length for a plan.
 *
 * <ul>
 *   <li>MONTHLY — invoice generated once per calendar month; period rolls forward on each cycle.</li>
 *   <li>ANNUAL  — one invoice per year. The billing job skips annual subscriptions
 *                 whose period_end is more than 30 days away. Proration on mid-year
 *                 cancellation issues a credit memo (negative-amount invoice).</li>
 * </ul>
 */
public enum BillingInterval {
    MONTHLY,
    ANNUAL
}
