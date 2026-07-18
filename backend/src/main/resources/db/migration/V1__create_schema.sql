-- ============================================================
-- V1 — Initial Schema
-- SubMeter: all tables, PKs, FKs, and CHECK constraints.
-- Tables ordered so FK dependencies are satisfied top-to-bottom.
--
-- NOTE: Every PK uses DEFAULT gen_random_uuid() so both
--       Hibernate (which pre-generates in Java) and direct SQL
--       inserts (seed script) produce valid UUIDs without
--       manual gen_random_uuid() calls in INSERT statements.
-- ============================================================

-- pgcrypto provides gen_random_uuid() on all PG versions < 13.
-- On PG 13+ gen_random_uuid() is built-in, but the extension is
-- harmless and keeps the migration portable.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ORGANIZATIONS
-- Root tenant entity. Every other table is scoped to an org.
-- ============================================================
CREATE TABLE organizations (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    name       TEXT        NOT NULL,
    slug       TEXT        NOT NULL,          -- URL-safe identifier, globally unique
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,                   -- soft-delete; null = active
    CONSTRAINT pk_organizations PRIMARY KEY (id),
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

-- ============================================================
-- USERS
-- Belongs to exactly one org (v1). Role = OWNER | ADMIN | MEMBER.
--
-- FLAG: failed_login_count and locked_until are NOT NULL / NOT NULL
-- because the rate-limiter logic in M2 reads them on every request;
-- a NULL would require extra null-checks that could be bypassed.
-- ============================================================
CREATE TABLE users (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id              UUID        NOT NULL,
    email               TEXT        NOT NULL,
    password_hash       TEXT        NOT NULL,
    role                TEXT        NOT NULL,
    email_verified      BOOLEAN     NOT NULL DEFAULT false,
    last_login_at       TIMESTAMPTZ,
    failed_login_count  INT         NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT fk_users_org      FOREIGN KEY (org_id)  REFERENCES organizations(id),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_role    CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

-- ============================================================
-- CUSTOMERS
-- An end-customer billed by the org.
-- email uniqueness is per-org (enforced at app layer + partial index in V2).
-- FLAG: email is NOT NULL — required for invoice delivery even in mock mode.
-- ============================================================
CREATE TABLE customers (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id     UUID        NOT NULL,
    name       TEXT        NOT NULL,
    email      TEXT        NOT NULL,
    phone      TEXT,                          -- optional; no format constraint in v1
    metadata   JSONB       NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_customers    PRIMARY KEY (id),
    CONSTRAINT fk_customers_org FOREIGN KEY (org_id) REFERENCES organizations(id)
);

-- ============================================================
-- PLANS
-- Pricing template. Pricing is immutable once subscribed; use
-- version snapshots (plan_version in subscriptions) for price changes.
--
-- FLAG: flat_amount has a DB CHECK enforcing NOT NULL when FLAT,
-- because a null flat price silently produces ₹0 invoices.
-- ============================================================
CREATE TABLE plans (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id           UUID        NOT NULL,
    name             TEXT        NOT NULL,
    description      TEXT,
    pricing_model    TEXT        NOT NULL,
    flat_amount      BIGINT,                  -- paisa; NOT NULL when pricing_model = 'FLAT'
    billing_interval TEXT        NOT NULL,
    trial_days       INT         NOT NULL DEFAULT 0,
    is_archived      BOOLEAN     NOT NULL DEFAULT false,
    version          INT         NOT NULL DEFAULT 1,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_plans PRIMARY KEY (id),
    CONSTRAINT fk_plans_org              FOREIGN KEY (org_id) REFERENCES organizations(id),
    CONSTRAINT chk_plans_pricing_model   CHECK (pricing_model    IN ('FLAT', 'TIERED', 'METERED')),
    CONSTRAINT chk_plans_billing_interval CHECK (billing_interval IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT chk_plans_flat_amount     CHECK (
        (pricing_model = 'FLAT' AND flat_amount IS NOT NULL AND flat_amount >= 0)
        OR pricing_model IN ('TIERED', 'METERED')
    ),
    CONSTRAINT chk_plans_trial_days CHECK (trial_days >= 0)
);

-- ============================================================
-- PLAN TIERS
-- Used when pricing_model IN ('TIERED', 'METERED').
-- tier_order is 1-indexed ascending; the last tier has up_to = NULL (∞).
-- Tiers are append-only once a subscription references the plan version.
-- ============================================================
CREATE TABLE plan_tiers (
    id          UUID   NOT NULL DEFAULT gen_random_uuid(),
    plan_id     UUID   NOT NULL,
    tier_order  INT    NOT NULL,
    up_to       BIGINT,                       -- NULL = this tier has no upper bound
    unit_amount BIGINT NOT NULL,              -- paisa per unit
    flat_fee    BIGINT NOT NULL DEFAULT 0,    -- per-tier flat fee in paisa (Stripe-style)
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_plan_tiers        PRIMARY KEY (id),
    CONSTRAINT fk_plan_tiers_plan   FOREIGN KEY (plan_id) REFERENCES plans(id),
    CONSTRAINT uq_plan_tiers_order  UNIQUE (plan_id, tier_order),
    CONSTRAINT chk_plan_tiers_unit  CHECK (unit_amount >= 0),
    CONSTRAINT chk_plan_tiers_fee   CHECK (flat_fee >= 0)
);

-- ============================================================
-- SUBSCRIPTIONS
-- State machine: TRIAL → ACTIVE → PAST_DUE → CANCELED
--                ACTIVE → CANCELED (direct cancel)
--                PAST_DUE → ACTIVE (successful retry)
-- CANCELED is terminal.
--
-- FLAG: plan_version is NOT NULL — it is set at creation time by
-- copying plan.version. Without it, a plan upgrade would silently
-- re-price historical subscriptions.
-- ============================================================
CREATE TABLE subscriptions (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id               UUID        NOT NULL,
    customer_id          UUID        NOT NULL,
    plan_id              UUID        NOT NULL,
    plan_version         INT         NOT NULL,   -- snapshot; billing always uses this version
    status               TEXT        NOT NULL,
    trial_end_at         TIMESTAMPTZ,
    current_period_start TIMESTAMPTZ,
    current_period_end   TIMESTAMPTZ,
    canceled_at          TIMESTAMPTZ,
    cancellation_reason  TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_subscriptions         PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_org     FOREIGN KEY (org_id)      REFERENCES organizations(id),
    CONSTRAINT fk_subscriptions_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_subscriptions_plan    FOREIGN KEY (plan_id)     REFERENCES plans(id),
    CONSTRAINT chk_subscriptions_status CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELED'))
);

-- ============================================================
-- INVOICES
-- One per subscription per billing period.
-- UNIQUE (subscription_id, period_start) is the idempotency guard
-- for the nightly billing job — a crash + re-run will not double-invoice.
--
-- FLAG: due_at is NOT NULL — required for past-due detection query.
-- FLAG: total_cents NOT NULL DEFAULT 0 — a null total would cause
-- silent ₹0 Razorpay orders.
-- ============================================================
CREATE TABLE invoices (
    id                UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id            UUID        NOT NULL,
    subscription_id   UUID        NOT NULL,
    invoice_number    TEXT        NOT NULL,       -- INV-{YEAR}-{SEQ:04d}, org-scoped
    status            TEXT        NOT NULL DEFAULT 'DRAFT',
    period_start      TIMESTAMPTZ NOT NULL,
    period_end        TIMESTAMPTZ NOT NULL,
    subtotal_cents    BIGINT      NOT NULL DEFAULT 0,
    tax_cents         BIGINT      NOT NULL DEFAULT 0,   -- always 0 in v1; reserved for GST
    total_cents       BIGINT      NOT NULL DEFAULT 0,
    due_at            TIMESTAMPTZ NOT NULL,
    paid_at           TIMESTAMPTZ,
    razorpay_order_id TEXT,
    pdf_path          TEXT,                       -- relative path within storage root
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_invoices               PRIMARY KEY (id),
    CONSTRAINT fk_invoices_org           FOREIGN KEY (org_id)          REFERENCES organizations(id),
    CONSTRAINT fk_invoices_subscription  FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    CONSTRAINT uq_invoices_number        UNIQUE (invoice_number),
    CONSTRAINT uq_invoices_period        UNIQUE (subscription_id, period_start),  -- idempotency guard
    CONSTRAINT chk_invoices_status       CHECK (status IN ('DRAFT', 'OPEN', 'PAID', 'VOID', 'UNCOLLECTIBLE')),
    CONSTRAINT chk_invoices_amounts      CHECK (subtotal_cents >= 0 AND tax_cents >= 0 AND total_cents >= 0)
);

-- ============================================================
-- INVOICE LINE ITEMS
-- Immutable once written; never updated.
-- tier_detail stores the full per-tier breakdown for tiered/metered
-- plans so the PDF can show the customer exactly how the amount was
-- computed, even if the plan is later versioned.
-- ============================================================
CREATE TABLE invoice_line_items (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    invoice_id    UUID        NOT NULL,
    description   TEXT        NOT NULL,
    quantity      BIGINT      NOT NULL DEFAULT 1,
    unit_amount   BIGINT      NOT NULL,           -- paisa per unit
    amount        BIGINT      NOT NULL,           -- denormalized: quantity * unit_amount
    pricing_model TEXT        NOT NULL,
    tier_detail   JSONB,                          -- full tier breakdown; null for FLAT
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_invoice_line_items      PRIMARY KEY (id),
    CONSTRAINT fk_line_items_invoice      FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT chk_line_items_quantity    CHECK (quantity > 0),
    CONSTRAINT chk_line_items_amount      CHECK (amount >= 0),
    CONSTRAINT chk_line_items_pricing_model CHECK (pricing_model IN ('FLAT', 'TIERED', 'METERED'))
);

-- ============================================================
-- USAGE EVENTS
-- Append-only. Events after subscription cancellation are stored
-- with rejected_reason set rather than deleted — financial audit trail.
--
-- created_at serves as processed_at (when the API received the event).
-- occurred_at is the client-provided event timestamp (may be in the past).
--
-- FLAG: idempotency_key UNIQUE — DB-level guarantee; never rely solely
-- on app-layer check.
-- FLAG: quantity > 0 CHECK — a zero-quantity event is a no-op and
-- would silently corrupt usage totals.
-- ============================================================
CREATE TABLE usage_events (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id           UUID        NOT NULL,
    subscription_id  UUID        NOT NULL,
    event_type       TEXT        NOT NULL,
    quantity         BIGINT      NOT NULL,
    idempotency_key  TEXT        NOT NULL,         -- client-provided; UUID recommended
    occurred_at      TIMESTAMPTZ NOT NULL,         -- client-provided event time
    invoice_id       UUID,                         -- set by billing job when rolled into invoice
    rejected_reason  TEXT,                         -- e.g. 'subscription_canceled'
    metadata         JSONB       NOT NULL DEFAULT '{}',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_usage_events               PRIMARY KEY (id),
    CONSTRAINT fk_usage_events_org           FOREIGN KEY (org_id)          REFERENCES organizations(id),
    CONSTRAINT fk_usage_events_subscription  FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    CONSTRAINT fk_usage_events_invoice       FOREIGN KEY (invoice_id)      REFERENCES invoices(id),
    CONSTRAINT uq_usage_events_idempotency   UNIQUE (idempotency_key),
    CONSTRAINT chk_usage_events_quantity     CHECK (quantity > 0)
);

-- ============================================================
-- PAYMENTS
-- One row per Razorpay order. razorpay_payment_id is set on
-- payment.captured webhook. The unique partial index in V2
-- enforces webhook idempotency at the DB level (schema gap fix #1).
-- ============================================================
CREATE TABLE payments (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id               UUID        NOT NULL,
    invoice_id           UUID        NOT NULL,
    razorpay_order_id    TEXT        NOT NULL,
    razorpay_payment_id  TEXT,                    -- null until payment.captured fires
    amount_cents         BIGINT      NOT NULL,
    currency             TEXT        NOT NULL DEFAULT 'INR',
    status               TEXT        NOT NULL DEFAULT 'PENDING',
    failure_reason       TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_payments         PRIMARY KEY (id),
    CONSTRAINT fk_payments_org     FOREIGN KEY (org_id)      REFERENCES organizations(id),
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id)  REFERENCES invoices(id),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payments_amount CHECK (amount_cents > 0)
);

-- ============================================================
-- AUDIT LOG
-- Append-only. actor_id is NULL for SYSTEM actions (billing job).
-- ip_address stored as TEXT (not INET) to avoid psql-operator
-- import overhead; validated at application layer.
-- ============================================================
CREATE TABLE audit_log (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id      UUID        NOT NULL,
    actor_id    UUID,                             -- NULL = system action
    actor_type  TEXT        NOT NULL,
    entity_type TEXT        NOT NULL,
    entity_id   UUID        NOT NULL,
    action      TEXT        NOT NULL,
    diff        JSONB,                            -- before/after for UPDATE; full obj for CREATE
    ip_address  TEXT,
    user_agent  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_log          PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_org      FOREIGN KEY (org_id)    REFERENCES organizations(id),
    CONSTRAINT fk_audit_log_actor    FOREIGN KEY (actor_id)  REFERENCES users(id),
    CONSTRAINT chk_audit_log_actor_type CHECK (actor_type IN ('USER', 'SYSTEM')),
    CONSTRAINT chk_audit_log_action     CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE'))
);

-- ============================================================
-- INVOICE SEQUENCES
-- Org-scoped, year-scoped sequence for invoice numbering.
-- Format: INV-{YEAR}-{SEQ:04d}  e.g. INV-2025-0001
-- Used exclusively by the next_invoice_number() function in V3.
-- ============================================================
CREATE TABLE invoice_sequences (
    org_id   UUID NOT NULL,
    year     INT  NOT NULL,
    last_seq INT  NOT NULL DEFAULT 0,
    CONSTRAINT pk_invoice_sequences     PRIMARY KEY (org_id, year),
    CONSTRAINT fk_invoice_seq_org       FOREIGN KEY (org_id) REFERENCES organizations(id)
);
