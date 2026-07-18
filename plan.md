# SubMeter — Plan v0.1
> **Status:** Awaiting approval before any implementation code is written.

---

## 0. Design Direction (commit before any UI code)

### Reference Products
**Vercel's operational density + Stripe's financial-data typography.**

- **Vercel**: ink-dark canvas, razor-thin UI chrome, mono-spaced identifiers, no decorative illustration, status conveyed entirely through color and weight — not icons or badges.  
- **Stripe**: tabular numbers, left-aligned labels, right-aligned amounts, chart axes that breathe, no chart legend unless there are >2 series, dollar amounts rendered with distinct integer / fractional weight split.

The result is a dashboard that feels like a *tool*, not a marketing site — fast to scan, zero ambient decoration, every pixel earning its rent.

---

### Design System Tokens

#### Color Palette

| Token | Hex | Usage |
|---|---|---|
| `--gray-950` | `#0A0A0B` | Page canvas |
| `--gray-900` | `#111113` | Sidebar, nav surface |
| `--gray-800` | `#1C1C1F` | Cards, table rows |
| `--gray-700` | `#2A2A2E` | Borders, dividers |
| `--gray-500` | `#6B6B72` | Muted labels, placeholder |
| `--gray-300` | `#C4C4CC` | Body text |
| `--gray-100` | `#F0F0F2` | Primary text |
| `--accent`   | `#00C48C` | ONE accent: CTA, positive trend, active state |
| `--accent-dim`| `#00C48C1A` | Accent ghost: pill backgrounds, row highlights |
| `--danger`   | `#EF4444` | Error, past_due, failed payment |
| `--warn`     | `#F59E0B` | trial status, approaching limit |

> **Rule**: No other color is permitted. Every foreground/background combination must achieve WCAG AA (>= 4.5:1) contrast. The accent hue `#00C48C` is a muted teal-green — not a neon, not a brand blue. It reads as "healthy revenue" without shouting.

#### Typography

Font: **JetBrains Mono** (self-hosted WOFF2) for all numeric data. **IBM Plex Sans** (self-hosted WOFF2) for prose/labels.

| Token | Size | Weight | Line-height | Usage |
|---|---|---|---|---|
| `--text-hero` | 2.5rem / 40px | 500 | 1.15 | KPI numbers |
| `--text-xl` | 1.25rem / 20px | 500 | 1.3 | Section headers |
| `--text-base` | 0.9375rem / 15px | 400 | 1.5 | Body, table cells |
| `--text-sm` | 0.8125rem / 13px | 400 | 1.4 | Labels, meta |
| `--text-xs` | 0.6875rem / 11px | 500 | 1.2 | Badges, status pills (uppercase tracked) |
| `--text-mono` | 0.875rem / 14px | JetBrains Mono 400 | 1.4 | IDs, amounts, code |

#### Spacing Scale (8px base)

`4 · 8 · 12 · 16 · 24 · 32 · 48 · 64 · 96`  
No fractions; no arbitrary values like `px-3` mixing with `px-4`.

#### Border Radius

| Token | Value | Usage |
|---|---|---|
| `--radius-sm` | 4px | Badges, status pills |
| `--radius-md` | 6px | Cards, inputs |
| `--radius-lg` | 10px | Modals |
| `--radius-none` | 0 | Table rows, sidebar nav items |

#### Signature Detail — Currency Rendering

All monetary amounts use **split-weight tabular rendering**:  
The integer part is rendered in `--text-hero` weight 500, and the fractional cents are rendered at 60% opacity and `--text-xl` size, in `font-variant-numeric: tabular-nums`. Example:

```
$12,450  42    <-  integer bold, cents lighter, no period halo
```

This is the one visual fingerprint that makes the dashboard look *designed*: a glance at any number tells you its order of magnitude without reading it.

#### Chart Style

- Recharts `AreaChart` with a **vertical gradient fill** from `accent @ 20% opacity` at top to `transparent` at bottom.  
- Axis labels: `--text-xs` in `--gray-500`, no grid lines on Y-axis, a single hairline horizontal rule at `y=0`.  
- Tooltips: dark card (`--gray-800` background, `--gray-700` border, `--text-mono` for numbers).  
- No chart titles — the section heading above the chart *is* the title.  
- Dots shown only on hover (no persistent dots on line).

---

### Dashboard ASCII Mockup

```
+-----------------------------------------------------------------------------+
|  > SubMeter          [Org: Acme Corp v]                    [JD] Settings    |
+----------+------------------------------------------------------------------+
| Overview |  MRR            ARPU           Churn           Active Subs       |
| Customers|  $12,450 42     $248 00        2.3%            51               |
| Plans    |  ^ +4.2% mo/mo  ^ +1.1%        v -0.4%         ^ +3             |
| Invoices |                                                                  |
| Audit Log|  -------------------------------------------------------- [90d v]|
|          |  ################################################################|
|          |  ####  area chart (MRR over time)  ############################  |
|          |                                                                  |
|          |  Recent Invoices                          [+ New Invoice]        |
|          |  +-----------------------------------------------------------+   |
|          |  | #  Customer          Period      Amount    Status         |   |
|          |  | -- -------------     ----------  --------  -------------- |   |
|          |  | 47 Globex Corp       Jun 2025    $1,200 00  PAID  *      |   |
|          |  | 46 Initech LLC       Jun 2025      $480 00  PAID  *      |   |
|          |  | 45 Umbrella Co.      Jun 2025    $3,600 00  DRAFT *      |   |
|          |  | 44 Wayne Ent.        Jun 2025      $960 00  PAST_DUE *   |   |
|          |  +-----------------------------------------------------------+   |
+----------+------------------------------------------------------------------+
```

- Sidebar: `--gray-900` background, no icons (text labels only), active item has a 2px left accent bar in `--accent`.  
- KPI row: 4 numbers with trend deltas; negative trend shows `--danger`, positive shows `--accent`.  
- Table: alternating row background `--gray-950` / `--gray-800`; status is a small colored circle (*) + uppercase `--text-xs` label — not a pill/chip.  
- "PAST_DUE" row gets a full-row subtle left border in `--danger`.

---

## 1. User Stories & Acceptance Criteria

### 1.1 Users & Auth

| Story | Acceptance Criteria |
|---|---|
| As a new user I can sign up with email + password | Account created; org created; role = OWNER; mock verification link logged to console; returns JWT cookie |
| As a user I can log in | Valid credentials return new JWT cookie (rotated); brute-force lockout after 5 failures/15min |
| As a user I can refresh my session without re-login | Refresh endpoint exchanges old JWT for new one; old token invalidated |
| As an owner I can invite a team member | Invitation link logged to console; invitee signs up linked to same org with role MEMBER |
| As a member I cannot delete customers | API returns 403; frontend hides the action but still enforces server-side |

### 1.2 Customers

| Story | Acceptance Criteria |
|---|---|
| As an admin I can create a customer | POST /customers returns 201 with full record; audit log entry written |
| As an admin I can edit a customer | PUT returns updated record; audit log entry written |
| As an admin I can soft-delete a customer | Deleted_at set; customer no longer in list; subscriptions moved to CANCELED; audit log entry |
| As a member I can view customers | GET /customers returns paginated list scoped to org; search by name/email |
| As a member I can view a customer's subscription history | Returns all subscriptions including canceled ones |

### 1.3 Plans

| Story | Acceptance Criteria |
|---|---|
| As an admin I can create a plan (flat/tiered/metered) | POST /plans returns 201; pricing_model validated; no subscribers yet |
| As an admin I can update plan metadata | Only metadata (name/description) can change after a subscriber exists; price changes require version snapshot |
| As an admin I can archive a plan | Archived plans accept no new subscriptions; existing subs continue at locked snapshot |
| As a member I can list plans | Returns all active plans with pricing detail |

### 1.4 Subscriptions

| Story | Acceptance Criteria |
|---|---|
| As an admin I can create a subscription | POST /subscriptions; status defaults to TRIAL; trial_end_at set per plan config |
| As an admin I can activate a subscription | Status transitions TRIAL -> ACTIVE; current_period_start/end set |
| As the billing engine I can mark a sub past due | Status: ACTIVE -> PAST_DUE when invoice exceeds payment window |
| As an admin I can cancel a subscription | ACTIVE/TRIAL/PAST_DUE -> CANCELED; cancellation recorded; usage events frozen |
| Status machine enforces valid transitions only | Invalid transition (e.g. CANCELED -> ACTIVE) returns 422 with reason |

### 1.5 Usage Events

| Story | Acceptance Criteria |
|---|---|
| As a customer's system I can ingest a usage event | POST /usage-events; idempotency key prevents double-counting; returns 202 |
| Usage events are validated | subscription_id must be ACTIVE; event_type must exist on the plan; quantity > 0 |
| Usage events after cancellation are rejected | Returns 422 "Subscription not active" |
| Usage can be queried | GET /usage-events?subscription_id=X&period=... returns paginated list |

### 1.6 Invoices

| Story | Acceptance Criteria |
|---|---|
| As the billing engine I generate a draft invoice nightly | One draft invoice per billing-period per subscription; line items computed from pricing model |
| As an admin I can view an invoice | GET /invoices/:id returns full line items with unit prices |
| As an admin I can export a PDF | GET /invoices/:id/pdf streams a PDF; streamed async, not blocking |
| As an admin I can initiate payment | POST /invoices/:id/pay creates a Razorpay order in test mode; returns order_id |
| As the Razorpay webhook I can mark an invoice paid | POST /webhooks/razorpay; signature verified; invoice marked PAID; payment row inserted |

### 1.7 Analytics

| Story | Acceptance Criteria |
|---|---|
| As an admin I can view MRR trend | GET /analytics/mrr?range=90d returns daily MRR array; computed server-side |
| As an admin I can view ARPU | MRR / active subscriber count per period |
| As an admin I can view churn % | Subs canceled this month / subs at start of month |

### 1.8 Audit Log

| Story | Acceptance Criteria |
|---|---|
| All mutations write audit entries | create/update/delete on every entity writes actor, entity_type, entity_id, diff, timestamp |
| As an owner I can query the audit log | GET /audit-log?entity_type=customer&entity_id=X; paginated |

---

## 2. Data Model

### Entity Relationship Summary

```
organizations
  +- users (org_id, role: OWNER|ADMIN|MEMBER)
  +- customers (org_id)
  |    +- subscriptions (customer_id, plan_id)
  |         +- usage_events (subscription_id)
  |         +- invoices (subscription_id)
  |              +- invoice_line_items (invoice_id)
  |              +- payments (invoice_id)
  +- plans (org_id)
  |    +- plan_tiers (plan_id)   <- for TIERED/METERED pricing
  +- audit_log (org_id)
```

### Table Definitions

#### `organizations`
```sql
id              UUID PK default gen_random_uuid()
name            TEXT NOT NULL
slug            TEXT NOT NULL UNIQUE
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
deleted_at      TIMESTAMPTZ
```

#### `users`
```sql
id              UUID PK
org_id          UUID NOT NULL REFERENCES organizations(id)
email           TEXT NOT NULL UNIQUE
password_hash   TEXT NOT NULL
role            TEXT NOT NULL CHECK(role IN ('OWNER','ADMIN','MEMBER'))
email_verified  BOOLEAN NOT NULL DEFAULT false
last_login_at   TIMESTAMPTZ
failed_login_count INT NOT NULL DEFAULT 0
locked_until    TIMESTAMPTZ
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
deleted_at      TIMESTAMPTZ

INDEX: (org_id, created_at)
```

#### `customers`
```sql
id              UUID PK
org_id          UUID NOT NULL REFERENCES organizations(id)
name            TEXT NOT NULL
email           TEXT NOT NULL
phone           TEXT
metadata        JSONB NOT NULL DEFAULT '{}'
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
deleted_at      TIMESTAMPTZ

INDEX: (org_id, created_at)
INDEX: (org_id, email)        <- for uniqueness check within org
```

#### `plans`
```sql
id              UUID PK
org_id          UUID NOT NULL REFERENCES organizations(id)
name            TEXT NOT NULL
description     TEXT
pricing_model   TEXT NOT NULL CHECK(pricing_model IN ('FLAT','TIERED','METERED'))
flat_amount     BIGINT            <- cents; NOT NULL if pricing_model = 'FLAT'
billing_interval TEXT NOT NULL CHECK(billing_interval IN ('MONTHLY','ANNUAL'))
trial_days      INT NOT NULL DEFAULT 0
is_archived     BOOLEAN NOT NULL DEFAULT false
version         INT NOT NULL DEFAULT 1
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()

INDEX: (org_id, created_at)
```

#### `plan_tiers`
```sql
id              UUID PK
plan_id         UUID NOT NULL REFERENCES plans(id)
tier_order      INT NOT NULL      <- 1-indexed sort for tier application
up_to           BIGINT            <- NULL means infinity (last tier)
unit_amount     BIGINT NOT NULL   <- cents per unit
flat_fee        BIGINT NOT NULL DEFAULT 0  <- per-tier flat fee (Stripe-style)

UNIQUE: (plan_id, tier_order)
```

#### `subscriptions`
```sql
id                   UUID PK
org_id               UUID NOT NULL REFERENCES organizations(id)
customer_id          UUID NOT NULL REFERENCES customers(id)
plan_id              UUID NOT NULL REFERENCES plans(id)
plan_version         INT NOT NULL    <- snapshot of plan.version at subscription time
status               TEXT NOT NULL CHECK(status IN ('TRIAL','ACTIVE','PAST_DUE','CANCELED'))
trial_end_at         TIMESTAMPTZ
current_period_start TIMESTAMPTZ
current_period_end   TIMESTAMPTZ
canceled_at          TIMESTAMPTZ
cancellation_reason  TEXT
created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()

INDEX: (org_id, created_at)
INDEX: (customer_id)
INDEX: (status, current_period_end)  <- for nightly billing job
```

#### `usage_events`
```sql
id               UUID PK
org_id           UUID NOT NULL REFERENCES organizations(id)
subscription_id  UUID NOT NULL REFERENCES subscriptions(id)
event_type       TEXT NOT NULL   <- e.g. 'api_calls', 'storage_gb'
quantity         BIGINT NOT NULL CHECK(quantity > 0)
idempotency_key  TEXT NOT NULL UNIQUE   <- prevents double-ingestion
occurred_at      TIMESTAMPTZ NOT NULL   <- client-provided timestamp
processed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
invoice_id       UUID REFERENCES invoices(id)  <- set when rolled into an invoice
metadata         JSONB NOT NULL DEFAULT '{}'

INDEX: (subscription_id, occurred_at)
INDEX: (org_id, created_at)
UNIQUE INDEX: idempotency_key
```

#### `invoices`
```sql
id                UUID PK
org_id            UUID NOT NULL REFERENCES organizations(id)
subscription_id   UUID NOT NULL REFERENCES subscriptions(id)
invoice_number    TEXT NOT NULL UNIQUE    <- e.g. INV-2025-0047
status            TEXT NOT NULL CHECK(status IN ('DRAFT','OPEN','PAID','VOID','UNCOLLECTIBLE'))
period_start      TIMESTAMPTZ NOT NULL
period_end        TIMESTAMPTZ NOT NULL
subtotal_cents    BIGINT NOT NULL DEFAULT 0
tax_cents         BIGINT NOT NULL DEFAULT 0
total_cents       BIGINT NOT NULL DEFAULT 0
due_at            TIMESTAMPTZ NOT NULL
paid_at           TIMESTAMPTZ
razorpay_order_id TEXT
pdf_path          TEXT            <- path to generated PDF in storage
created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()

INDEX: (org_id, created_at)
INDEX: (subscription_id, period_start)
UNIQUE: (subscription_id, period_start)   <- prevents double-invoicing
INDEX: (status, due_at)                   <- for past-due scanning
```

#### `invoice_line_items`
```sql
id            UUID PK
invoice_id    UUID NOT NULL REFERENCES invoices(id)
description   TEXT NOT NULL
quantity      BIGINT NOT NULL DEFAULT 1
unit_amount   BIGINT NOT NULL    <- cents
amount        BIGINT NOT NULL    <- = quantity * unit_amount (denormalized for PDF)
pricing_model TEXT NOT NULL
tier_detail   JSONB              <- full tier breakdown for audit trail
created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
```

#### `payments`
```sql
id                   UUID PK
org_id               UUID NOT NULL REFERENCES organizations(id)
invoice_id           UUID NOT NULL REFERENCES invoices(id)
razorpay_order_id    TEXT NOT NULL
razorpay_payment_id  TEXT
amount_cents         BIGINT NOT NULL
currency             TEXT NOT NULL DEFAULT 'INR'
status               TEXT NOT NULL CHECK(status IN ('PENDING','SUCCESS','FAILED','REFUNDED'))
failure_reason       TEXT
created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()

INDEX: (invoice_id)
INDEX: (org_id, created_at)
```

#### `audit_log`
```sql
id           UUID PK
org_id       UUID NOT NULL REFERENCES organizations(id)
actor_id     UUID REFERENCES users(id)   <- NULL for system actions (billing job)
actor_type   TEXT NOT NULL CHECK(actor_type IN ('USER','SYSTEM'))
entity_type  TEXT NOT NULL    <- 'customer','plan','subscription','invoice', etc.
entity_id    UUID NOT NULL
action       TEXT NOT NULL    <- 'CREATE','UPDATE','DELETE','STATUS_CHANGE'
diff         JSONB            <- before/after for UPDATE; full object for CREATE
ip_address   INET
user_agent   TEXT
created_at   TIMESTAMPTZ NOT NULL DEFAULT now()

INDEX: (org_id, created_at)
INDEX: (entity_type, entity_id, created_at)
```

### State Machine: Subscription Status

```
          +-----------+
          |   TRIAL   |------------------------------------------+
          +-----+-----+                                          |
                |  activate (manual or trial_end)                | cancel
                v                                                v
          +-----------+                                  +-------------+
          |  ACTIVE   |---- payment failed ------------> |  PAST_DUE   |
          +-----+-----+                                  +------+------+
                |  cancel                                       |  cancel / retry success
                v                                               v
          +------------+                                +-------------+
          |  CANCELED  | <-----------------------------|  PAST_DUE   |
          +------------+          cancel               +-------------+

Legend:
  PAST_DUE -> ACTIVE is valid (on successful retry payment)
  CANCELED is terminal — no transitions out
```

---

## 3. Design System — Dashboard Mockup

See Section 0 for full token table and ASCII layout. Three-column breakdown:

1. **Left sidebar (240px fixed)**: logo mark (text-based, no SVG blob), nav links, org switcher at top, user avatar (initials only, square 28px) at bottom.
2. **Main area**: KPI row (4 metrics), area chart below spanning full width, then a table of recent invoices with inline status, amount, and a "Pay" CTA per OPEN/PAST_DUE row.
3. **No right panel in v1** — keep it single-column to stay in scope.

---

## 4. Edge Cases & Failure Modes

### 4.1 Usage Events After Cancellation
- **Rule**: `POST /usage-events` validates `subscription.status == ACTIVE` before accepting.  
- **On cancel**: existing unprocessed events with `invoice_id IS NULL` are processed in the *final* billing run for that subscription's current period, then all subsequent events are rejected.  
- **Orphaned events** (events received after the final invoice closes): stored but flagged `rejected_reason = 'subscription_canceled'` — never deleted, kept for customer audit.

### 4.2 Failed Payment
1. Razorpay webhook fires with `payment.failed` event.
2. Payment row status set to `FAILED`, `failure_reason` populated.
3. Invoice status remains `OPEN`.
4. Subscription status transitions `ACTIVE -> PAST_DUE`.
5. Nightly job skips draft invoice generation for PAST_DUE subs until status resolves.
6. A retry window is configurable (default: 3 days). After retry window expires without payment, subscription moves to `CANCELED`.
7. Audit log captures each state change with the Razorpay order ID.

### 4.3 Plan Price Change Mid-Cycle
- **Rule**: Plan pricing is **immutable** once any subscription references it. An admin "changing" a plan actually creates a new plan version (increments `version`, archives old tiers, inserts new `plan_tiers`).
- Existing subscriptions retain `plan_version` snapshot — their invoices are always computed against the version they subscribed to.
- The UI shows "Updated plan available" on the subscription detail; admin can explicitly migrate a subscriber to the new version (creates a new subscription record, cancels the old one, prorates the transition day).
- **Proration calculation**: if a subscriber migrates mid-cycle, the final invoice for the old plan covers `period_start -> migration_date`; the first invoice for the new plan covers `migration_date -> period_end`. Both computed proportionally by day count.

### 4.4 Invoice Generation — Idempotency
- The nightly job selects subscriptions with `status IN ('ACTIVE','PAST_DUE')` and `current_period_end <= now() + 1 day` where no DRAFT/OPEN invoice exists for that period.
- A database-level unique constraint on `(subscription_id, period_start)` prevents double invoicing.
- If the job crashes mid-run, re-running it is safe (idempotent by constraint).

### 4.5 Razorpay Webhook Replay
- Webhook handler verifies HMAC-SHA256 signature before any DB write.
- Idempotency key = Razorpay `payment_id`; a second webhook with the same `payment_id` is a no-op.
- Webhook endpoint is rate-limited separately from the customer-facing API.

### 4.6 Soft-Delete Cascade on Customer
When `DELETE /customers/:id` is called:
1. `customers.deleted_at` set to now().
2. All ACTIVE/TRIAL subscriptions for this customer are transitioned to CANCELED (with `cancellation_reason = 'customer_deleted'`).
3. In-flight invoices (DRAFT/OPEN) are VOIDed.
4. PAID invoices are retained unchanged (financial record).
5. Usage events are retained (financial audit trail).
6. Audit log entry captures the cascade.

### 4.7 Annual Plan Prorated Monthly Invoicing
Annual plans generate **one invoice per year**, not monthly. The billing job skips annual subscriptions whose period end is > 30 days away. If an annual sub is canceled mid-year, proration is computed at day granularity and the remaining balance is issued as a credit memo (VOID the open invoice, issue a new invoice with negative amount for the unused days).

---

## 5. Explicit Assumptions

1. **Currency**: All amounts stored in **cents (integer)**. Default display currency is INR for Razorpay compatibility. A `currency` column on invoice/payment records allows future multi-currency but v1 only supports INR.
2. **Single org per user (v1)**: Users belong to exactly one organization. Org switching UI is stubbed but not functional.
3. **No self-serve plan upgrades**: Only admins can change a subscriber's plan. There is no customer-facing portal in v1.
4. **Tax is zero by default**: `tax_cents = 0` in all billing calculations; the column exists for future GST integration.
5. **PDF storage**: PDFs are written to `./storage/invoices/` on the local filesystem for dev. A `StorageProvider` interface is defined so S3/GCS can be swapped in without touching business logic.
6. **Email delivery is mocked**: All transactional emails are logged as structured JSON to stdout and optionally shown on a dev-only `/dev/emails` endpoint. No SMTP or third-party email service.
7. **Razorpay test mode only**: `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` are test-mode keys. The app will not start if these env vars are absent (fail-fast validation).
8. **Seed script**: `seed.sql` populates 3 organizations, 5 customers each, 3 plan types, 90 days of usage events and invoices, including one PAST_DUE and one CANCELED subscription.
9. **Time zone**: All `TIMESTAMPTZ` fields stored and processed in UTC. The UI formats dates in the browser's local time zone using `Intl.DateTimeFormat`.
10. **No soft-delete on `plans`**: Plans use `is_archived` flag. Users use `deleted_at` for GDPR erasure.

---

## 6. Open Questions

1. **Currency display**: Razorpay requires INR. Should dashboard KPI cards display in INR (Rs.) or USD ($) for a Western hiring panel? I'll default to **USD display with INR as the backend unit** unless you specify otherwise.

2. **Trial-to-active trigger**: Should trial -> active happen automatically when `trial_end_at` is reached (via the nightly job), or require manual admin action? I'll assume **automatic** for realism.

3. **Razorpay webhook local testing**: Without a public URL, Razorpay can't POST webhooks to localhost. Should I include ngrok setup instructions, or use a mock webhook endpoint (`POST /dev/mock-webhook/razorpay`) that simulates a successful payment for demo purposes?

4. **Invoice number format**: `INV-2025-0047` (year + sequential). Do you want org-scoped sequences (restart at 0001 per org) or a global sequence? Global is simpler; org-scoped looks more professional on PDFs.

5. **Plan migration UX**: Should the admin see a "Migrate subscriber to new plan" button on the subscription detail page, or is plan migration out of scope for v1?

6. **Annual billing**: Should annual plans invoice once per year (one large invoice) or monthly at 1/12th of the annual price?

7. **Multi-tenant isolation**: Should `org_id` be enforced via Row-Level Security (RLS) in PostgreSQL, or solely at the application layer? I'll default to **application-layer enforcement** with every query including an explicit `WHERE org_id = ?` — but I want your sign-off since it's a security decision.

8. **PDF library**: For Java: iText (LGPL) vs. Apache PDFBox vs. Flying Saucer (XHTML -> PDF). My preference is **iText Community (LGPL)** for its table layout support. Any objection?

9. **Hosting target**: Should I include a `docker-compose.prod.yml` targeting Railway/Render/Fly.io, or is Docker Compose local-only sufficient for the hiring panel demo?

10. **Invitation flow scope**: Should the invitation email mock be a full URL that auto-registers the invitee (with a token), or is multi-user RBAC purely for the API/role-enforcement demo with a single user in the seed?

---

## 7. Milestone Checklist (tracked after approval)

- [ ] M0: plan.md approved
- [ ] M1: Flyway migrations + JPA entities
- [ ] M2: Auth (signup/login/JWT/RBAC) with failing -> passing tests
- [ ] M3: CRUD — Customers, Plans, Subscriptions + state machine
- [ ] M4: Usage ingestion + billing engine (tests first)
- [ ] M5: Invoices, PDF export, Razorpay test-mode payment
- [ ] M6: Analytics endpoints + Next.js dashboard (4 states + seed data)
- [ ] M7: Audit log + A11y + responsive + Lighthouse >= 90
- [ ] Review Pass (Prompt 8) after each milestone

---

*End of plan.md — awaiting your approval before any implementation code is written.*
