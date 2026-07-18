-- ============================================================
-- V6 — Add Settings, API Keys, Team Management, and Extended Audit Logs
-- ============================================================

-- 1. Extend organizations table
ALTER TABLE organizations 
ADD COLUMN logo_url TEXT,
ADD COLUMN timezone TEXT NOT NULL DEFAULT 'UTC',
ADD COLUMN currency TEXT NOT NULL DEFAULT 'INR',
ADD COLUMN support_email TEXT,
ADD COLUMN default_tax_rate BIGINT,
ADD COLUMN invoice_prefix TEXT,
ADD COLUMN invoice_footer TEXT,
ADD COLUMN company_website TEXT,
ADD COLUMN company_address TEXT,
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 2. Organization Members (Multi-tenancy and robust team management)
CREATE TABLE organization_members (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id     UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role       TEXT        NOT NULL,
    status     TEXT        NOT NULL DEFAULT 'ACTIVE',
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_organization_members PRIMARY KEY (id),
    CONSTRAINT fk_org_members_org      FOREIGN KEY (org_id)  REFERENCES organizations(id),
    CONSTRAINT fk_org_members_user     FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_org_members_user_org UNIQUE (org_id, user_id),
    CONSTRAINT chk_org_members_role    CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

-- 3. API Keys (with Base62 Prefix & SHA-256 Hashing)
CREATE TABLE api_keys (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id       UUID        NOT NULL,
    key_id       TEXT        NOT NULL,  -- Public identifier, e.g. "3a9f8b"
    prefix       TEXT        NOT NULL,  -- e.g. "sk_live_"
    key_hash     TEXT        NOT NULL,  -- SHA-256 hash of the generated secret
    last_4       TEXT        NOT NULL,  -- Last 4 chars for UI display
    name         TEXT        NOT NULL,
    scopes       TEXT[]      NOT NULL DEFAULT '{}',
    environment  TEXT        NOT NULL DEFAULT 'PRODUCTION',
    created_by   UUID,
    last_used_at TIMESTAMPTZ,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT pk_api_keys          PRIMARY KEY (id),
    CONSTRAINT fk_api_keys_org      FOREIGN KEY (org_id) REFERENCES organizations(id),
    CONSTRAINT fk_api_keys_creator  FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uq_api_keys_key_id   UNIQUE (key_id),
    CONSTRAINT uq_api_keys_hash     UNIQUE (key_hash),
    CONSTRAINT chk_api_keys_env     CHECK (environment IN ('PRODUCTION', 'SANDBOX'))
);

-- 4. Invitations
CREATE TABLE invitations (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    org_id      UUID        NOT NULL,
    email       TEXT        NOT NULL,
    role        TEXT        NOT NULL,
    token_hash  TEXT        NOT NULL, -- SHA-256
    status      TEXT        NOT NULL DEFAULT 'PENDING',
    invited_by  UUID        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT pk_invitations           PRIMARY KEY (id),
    CONSTRAINT fk_invitations_org       FOREIGN KEY (org_id)     REFERENCES organizations(id),
    CONSTRAINT fk_invitations_inviter   FOREIGN KEY (invited_by) REFERENCES users(id),
    CONSTRAINT chk_invitations_role     CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT chk_invitations_status   CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);

-- Unique index to prevent multiple pending invitations for the same email
CREATE UNIQUE INDEX uq_invitations_pending_email ON invitations (org_id, email) WHERE status = 'PENDING';

-- 5. Extend Audit Logs
ALTER TABLE audit_log 
ADD COLUMN request_id TEXT,
ADD COLUMN correlation_id TEXT,
ADD COLUMN resource_type TEXT,
ADD COLUMN resource_name TEXT,
ADD COLUMN success BOOLEAN,
ADD COLUMN duration_ms BIGINT,
ADD COLUMN old_value JSONB,
ADD COLUMN new_value JSONB;

-- Since 'diff' is replaced by old_value/new_value, we can migrate existing diffs if they exist, or just leave it.
-- Let's migrate 'diff' if it exists. (Assuming diff has 'before' and 'after' based on our UI fix)
UPDATE audit_log
SET old_value = diff->'before',
    new_value = diff->'after'
WHERE diff IS NOT NULL AND (diff ? 'before' OR diff ? 'after');

-- For CREATE actions, the full object was stored in diff directly.
UPDATE audit_log
SET new_value = diff
WHERE diff IS NOT NULL AND NOT (diff ? 'before' OR diff ? 'after') AND action = 'CREATE';

-- Drop the old diff column
ALTER TABLE audit_log DROP COLUMN diff;
