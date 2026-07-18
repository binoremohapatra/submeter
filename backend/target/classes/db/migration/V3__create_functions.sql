-- ============================================================
-- V3 — Database Functions and Triggers
-- ============================================================

-- ─── Invoice sequence function ───────────────────────────────────────────────
-- Atomically increments the org+year counter and returns a formatted
-- invoice number: INV-{YEAR}-{SEQ:04d}  e.g. INV-2025-0042
--
-- The ON CONFLICT ... DO UPDATE ... RETURNING pattern is atomic
-- under PostgreSQL's row-level locking; no separate SELECT needed.
-- Safe for concurrent billing job runs.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION next_invoice_number(p_org_id UUID, p_year INT)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_seq INT;
BEGIN
    INSERT INTO invoice_sequences (org_id, year, last_seq)
    VALUES (p_org_id, p_year, 1)
    ON CONFLICT (org_id, year) DO UPDATE
        SET last_seq = invoice_sequences.last_seq + 1
    RETURNING last_seq INTO v_seq;

    RETURN 'INV-' || p_year || '-' || LPAD(v_seq::TEXT, 4, '0');
END;
$$;

-- ─── Generic updated_at trigger function ─────────────────────────────────────
-- Applied to every table that has an updated_at column.
-- Hibernate's @UpdateTimestamp also sets this field before the UPDATE,
-- so the trigger is a belt-and-suspenders fallback for direct SQL writes
-- (seed script, admin console, future migrations).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

-- ─── Apply updated_at trigger to every mutable table ─────────────────────────
CREATE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_plans_updated_at
    BEFORE UPDATE ON plans
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_plan_tiers_updated_at
    BEFORE UPDATE ON plan_tiers
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_invoice_line_items_updated_at
    BEFORE UPDATE ON invoice_line_items
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_usage_events_updated_at
    BEFORE UPDATE ON usage_events
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_audit_log_updated_at
    BEFORE UPDATE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
