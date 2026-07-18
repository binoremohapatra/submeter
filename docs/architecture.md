# SubMeter — Architecture Decision Record: Multi-Tenancy Isolation

## Decision: Application-Layer `WHERE org_id = ?` (Current)

### Context

SubMeter is a multi-tenant SaaS where every table (except `organizations` itself) is
scoped to an `org_id`. The system must ensure that an authenticated user from Org A
can never read or write data belonging to Org B, even if they guess a valid UUID.

Two implementation options were evaluated:

---

## Option A — Application-Layer Enforcement (CURRENT IMPLEMENTATION)

Every repository query explicitly includes `WHERE org_id = :orgId`, where `:orgId`
is extracted from the server-side JWT claim (verified against the DB on privileged
actions — never trusted from the client request body).

**How it works:**

1. Auth filter validates the JWT and resolves `orgId` from the `users` table (not from
   a JWT claim alone) on every request.
2. The resolved `orgId` is placed in a `SecurityContext` object available to all
   service-layer methods.
3. Every `JpaRepository` method that lists or fetches records includes `org_id = ?`
   as a parameter — this is validated in the Prompt 8 review pass after each milestone.
4. Native queries in analytics endpoints are reviewed for missing `WHERE org_id = ?`
   as part of the standard review checklist.

**Trade-offs:**

| | Application-Layer |
|---|---|
| Implementation complexity | Low — no extra migration, works with standard JPA |
| Dev velocity | High — no context-switching between SQL and Java |
| Correctness risk | Medium — a developer must remember to add the filter; a code review catches it |
| Defense in depth | Single layer — a query bug exposes cross-tenant data |
| Audit surface | Low — no DB-level enforcement record |

**Mitigations in place:**
- Prompt 8 review pass explicitly hunts for any query missing `org_id` filter.
- Integration tests assert that a user from Org A cannot fetch data from Org B.
- All API endpoints that accept `org_id` in the request body re-verify it against
  the authenticated user's org before processing.

---

## Option B — PostgreSQL Row-Level Security (DEFERRED)

RLS policies on each table enforce isolation at the database layer. The current
database user (`submeter`) can only see rows where `org_id` matches the session
variable `app.current_org_id`, set at connection time.

**How it would work:**

```sql
-- Example RLS policy
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

CREATE POLICY customers_org_isolation ON customers
    USING (org_id = current_setting('app.current_org_id')::UUID);

-- Application sets at connection open:
SET LOCAL app.current_org_id = '<org-uuid>';
```

**Why it was deferred:**

1. **Flyway complexity**: RLS policies require a separate DB user for the app vs. admin
   (migrations must run as a superuser, app must run as a restricted user). This adds
   2-3 migration files and a Docker Compose change that would consume ~1 day of the
   build timeline.

2. **Hibernate incompatibility**: Hibernate's connection pool (`HikariCP`) reuses
   connections; the `SET LOCAL app.current_org_id` must be set on every statement
   in the same transaction. This requires a custom `org.hibernate.engine.jdbc.connections.spi.ConnectionProvider`
   or a `@TransactionEventListener` hook — non-trivial without careful testing.

3. **v1 scope discipline**: Finish beats features on the rubric. The app-layer approach
   is correct and reviewable; RLS is a hardening measure, not a correctness requirement
   for a portfolio demo with trusted code paths.

**When to implement RLS:**
- If SubMeter moves to production with real customer data.
- If a penetration test is required (RLS survives SQL injection; app-layer does not).
- If a second team writes queries against the same DB without going through the API.

**References:**
- [PostgreSQL RLS Documentation](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Hibernate Custom Connection Provider](https://docs.jboss.org/hibernate/orm/6.5/userguide/html_single/Hibernate_User_Guide.html#database-connection-handling)

---

## Other Architecture Decisions

### PDF Generation: OpenPDF (LGPL)

iText Community (LGPL) was approved in plan.md. OpenPDF is the maintained LGPL fork
of iText 5, compatible with the same API. iText 7/8 is now AGPL, which would require
open-sourcing the entire application if distributed. OpenPDF avoids this constraint.

### Invoice Number Sequence: DB Function

`next_invoice_number(org_id, year)` uses PostgreSQL's `ON CONFLICT DO UPDATE ... RETURNING`
pattern, which is atomic under row-level locking. No `SELECT FOR UPDATE` is needed.
This is safe for concurrent billing job runs within the same org.

### Soft-Delete: Explicit Filters (no `@SQLRestriction`)

Hibernate's `@SQLRestriction` (formerly `@Where`) is applied as a DB-level filter on all
queries for that entity. It can be bypassed by native queries and is invisible to reviewers
reading JPQL. We use explicit `WHERE deleted_at IS NULL` in every repository method instead,
making the filter visible and auditable.
