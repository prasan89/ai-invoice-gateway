-- Phase 17: AI Finance Copilot

CREATE TABLE copilot_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id         UUID NOT NULL,
    title           VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE copilot_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID NOT NULL REFERENCES copilot_sessions(id) ON DELETE CASCADE,
    role        VARCHAR(10) NOT NULL,   -- USER, ASSISTANT
    content     TEXT NOT NULL,
    metadata    JSONB,                  -- SQL generated, invoice_ids referenced, chart_type, etc.
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_copilot_sessions_org ON copilot_sessions(organization_id, updated_at DESC);
CREATE INDEX idx_copilot_messages_session ON copilot_messages(session_id, created_at ASC);

-- Phase 18: Production Hardening – object storage tracking
CREATE TABLE storage_objects (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    invoice_id       UUID REFERENCES invoices(id) ON DELETE SET NULL,
    bucket           VARCHAR(120) NOT NULL,
    object_key       VARCHAR(500) NOT NULL UNIQUE,
    content_type     VARCHAR(120),
    size_bytes       BIGINT,
    checksum_sha256  VARCHAR(64),
    storage_backend  VARCHAR(20) NOT NULL DEFAULT 'local',  -- local, s3, gcs, azure
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_storage_objects_org     ON storage_objects(organization_id);
CREATE INDEX idx_storage_objects_invoice ON storage_objects(invoice_id);
