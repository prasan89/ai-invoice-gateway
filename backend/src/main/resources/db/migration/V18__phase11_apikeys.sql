-- Phase 11: API keys for external integrations
CREATE TABLE api_keys (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    name             VARCHAR(200) NOT NULL,
    key_hash         VARCHAR(64) NOT NULL UNIQUE,
    scopes           JSONB NOT NULL DEFAULT '[]',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at     TIMESTAMP WITH TIME ZONE,
    revoked_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_api_keys_org       ON api_keys(organization_id);
CREATE INDEX idx_api_keys_key_hash  ON api_keys(key_hash) WHERE revoked_at IS NULL;
