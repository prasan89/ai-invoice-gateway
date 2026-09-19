-- Phase 13: Multi-tenant SaaS users & sessions
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS plan           VARCHAR(20) NOT NULL DEFAULT 'FREE',
    ADD COLUMN IF NOT EXISTS api_call_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_users      INTEGER NOT NULL DEFAULT 5;

CREATE TABLE users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    email            VARCHAR(320) NOT NULL UNIQUE,
    password_hash    VARCHAR(200) NOT NULL,
    role             VARCHAR(20) NOT NULL DEFAULT 'ANALYST',
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_sessions (
    session_token  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_email       ON users(email);
CREATE INDEX idx_users_org         ON users(organization_id);
CREATE INDEX idx_sessions_user     ON user_sessions(user_id);
CREATE INDEX idx_sessions_expires  ON user_sessions(expires_at);
