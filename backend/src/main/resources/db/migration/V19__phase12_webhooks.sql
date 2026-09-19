-- Phase 12: Webhooks and event platform
CREATE TABLE webhook_subscriptions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    url              VARCHAR(2000) NOT NULL,
    events           TEXT[] NOT NULL,
    secret           VARCHAR(100) NOT NULL,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE webhook_deliveries (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id  UUID NOT NULL REFERENCES webhook_subscriptions(id) ON DELETE CASCADE,
    event_type       VARCHAR(80) NOT NULL,
    payload          JSONB NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts         INTEGER NOT NULL DEFAULT 0,
    last_attempt_at  TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_webhook_subs_org     ON webhook_subscriptions(organization_id) WHERE active = TRUE;
CREATE INDEX idx_webhook_del_pending  ON webhook_deliveries(status) WHERE status = 'PENDING';
