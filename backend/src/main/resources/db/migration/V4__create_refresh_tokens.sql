-- ============================================================
-- V4 — Refresh Tokens
-- Opaque UUID tokens stored in DB for proper session invalidation.
-- JWT access tokens (15 min) cannot be revoked; refresh tokens can.
-- ============================================================
CREATE TABLE refresh_tokens (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    token       TEXT        NOT NULL,         -- UUID4 opaque string; never reused
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,                  -- null = active; non-null = revoked
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_refresh_tokens          PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user     FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_refresh_tokens_token    UNIQUE (token)
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

-- Partial index: only active (non-revoked) tokens need fast lookup
CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens (token)
    WHERE revoked_at IS NULL;

CREATE TRIGGER trg_refresh_tokens_updated_at
    BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
