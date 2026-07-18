package com.submeter.entity.enums;

/**
 * RBAC role for a user within an organization.
 * <p>
 * Permission matrix (enforced server-side in M2 RBAC interceptor):
 * <ul>
 *   <li>OWNER  — full access; can invite/remove users, change org settings.</li>
 *   <li>ADMIN  — CRUD on customers/plans/subscriptions/invoices; cannot manage users.</li>
 *   <li>MEMBER — read-only on all resources; cannot mutate.</li>
 * </ul>
 */
public enum Role {
    OWNER,
    ADMIN,
    MEMBER
}
