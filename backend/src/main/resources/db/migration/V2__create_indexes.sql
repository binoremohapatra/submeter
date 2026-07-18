-- ============================================================
-- V2 — Indexes
-- All query-pattern indexes separated from schema for clarity.
-- Rule: every paginated list has (org_id, created_at).
-- Rule: every FK used as a filter has its own index.
-- ============================================================

-- ─── USERS ──────────────────────────────────────────────────────────────────
CREATE INDEX idx_users_org_created
    ON users (org_id, created_at DESC);

-- ─── CUSTOMERS ──────────────────────────────────────────────────────────────
CREATE INDEX idx_customers_org_created
    ON customers (org_id, created_at DESC);

-- Per-org email lookup (uniqueness enforced at app layer with this index)
CREATE INDEX idx_customers_org_email
    ON customers (org_id, email)
    WHERE deleted_at IS NULL;

-- Full-text search index for customer name (used in M3 server-side search)
CREATE INDEX idx_customers_name_search
    ON customers USING gin (to_tsvector('english', name))
    WHERE deleted_at IS NULL;

-- ─── PLANS ──────────────────────────────────────────────────────────────────
CREATE INDEX idx_plans_org_created
    ON plans (org_id, created_at DESC);

CREATE INDEX idx_plans_org_active
    ON plans (org_id, is_archived)
    WHERE is_archived = false;

-- ─── SUBSCRIPTIONS ──────────────────────────────────────────────────────────
CREATE INDEX idx_subscriptions_org_created
    ON subscriptions (org_id, created_at DESC);

CREATE INDEX idx_subscriptions_customer
    ON subscriptions (customer_id);

-- Critical for the nightly billing job: find subscriptions approaching period end
CREATE INDEX idx_subscriptions_status_period_end
    ON subscriptions (status, current_period_end)
    WHERE status IN ('ACTIVE', 'PAST_DUE');

CREATE INDEX idx_subscriptions_trial_end
    ON subscriptions (trial_end_at)
    WHERE status = 'TRIAL';

-- ─── INVOICES ───────────────────────────────────────────────────────────────
CREATE INDEX idx_invoices_org_created
    ON invoices (org_id, created_at DESC);

-- Used by billing job to check for existing draft before creating a new one
CREATE INDEX idx_invoices_subscription_period
    ON invoices (subscription_id, period_start);

-- Past-due scanner
CREATE INDEX idx_invoices_status_due
    ON invoices (status, due_at)
    WHERE status IN ('OPEN', 'DRAFT');

-- ─── INVOICE LINE ITEMS ─────────────────────────────────────────────────────
CREATE INDEX idx_line_items_invoice
    ON invoice_line_items (invoice_id);

-- ─── USAGE EVENTS ───────────────────────────────────────────────────────────
CREATE INDEX idx_usage_events_subscription_occurred
    ON usage_events (subscription_id, occurred_at DESC);

CREATE INDEX idx_usage_events_org_created
    ON usage_events (org_id, created_at DESC);

-- Partial index: only un-billed events (those without an invoice yet)
CREATE INDEX idx_usage_events_unbilled
    ON usage_events (subscription_id, occurred_at)
    WHERE invoice_id IS NULL AND rejected_reason IS NULL;

-- ─── PAYMENTS ───────────────────────────────────────────────────────────────
CREATE INDEX idx_payments_invoice
    ON payments (invoice_id);

CREATE INDEX idx_payments_org_created
    ON payments (org_id, created_at DESC);

-- ── Schema gap fix #1 ────────────────────────────────────────────────────────
-- Nullable-safe partial unique index on razorpay_payment_id.
-- Standard UNIQUE would reject two NULLs in some engines; this partial index
-- only applies to non-null values, correctly enforcing webhook idempotency
-- while allowing multiple PENDING payments (before any webhook fires).
CREATE UNIQUE INDEX uq_payments_razorpay_payment_id
    ON payments (razorpay_payment_id)
    WHERE razorpay_payment_id IS NOT NULL;

-- ─── AUDIT LOG ──────────────────────────────────────────────────────────────
CREATE INDEX idx_audit_log_org_created
    ON audit_log (org_id, created_at DESC);

-- Composite: look up all audit events for a specific entity
CREATE INDEX idx_audit_log_entity
    ON audit_log (entity_type, entity_id, created_at DESC);
