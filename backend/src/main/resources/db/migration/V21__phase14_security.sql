-- Phase 14: Security audit event log
CREATE TABLE auth_events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID,
    user_id          UUID,
    event_type       VARCHAR(40) NOT NULL,
    actor_email      VARCHAR(320),
    ip_address       VARCHAR(45),
    user_agent       TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_auth_events_org  ON auth_events(organization_id, created_at DESC);
CREATE INDEX idx_auth_events_user ON auth_events(user_id, created_at DESC);
