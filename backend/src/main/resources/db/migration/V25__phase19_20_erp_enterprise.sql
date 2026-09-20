-- Phase 19: Real ERP Integrations – extended config
ALTER TABLE erp_sync_jobs
    ADD COLUMN IF NOT EXISTS erp_system   VARCHAR(40),
    ADD COLUMN IF NOT EXISTS sync_type    VARCHAR(40) DEFAULT 'INVOICE_PUSH',
    ADD COLUMN IF NOT EXISTS error_detail TEXT;

CREATE TABLE erp_connections (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    erp_system       VARCHAR(40) NOT NULL,  -- TALLY, ZOHO_BOOKS, SAP, GENERIC_REST
    display_name     VARCHAR(120) NOT NULL,
    config           JSONB NOT NULL DEFAULT '{}',   -- encrypted connection params
    status           VARCHAR(20) NOT NULL DEFAULT 'DISCONNECTED',
    last_synced_at   TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(organization_id, erp_system)
);

CREATE INDEX idx_erp_connections_org ON erp_connections(organization_id);

-- Phase 20: Enterprise – SSO & approval chains
CREATE TABLE sso_configurations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL UNIQUE,
    provider         VARCHAR(20) NOT NULL,  -- SAML, OIDC
    entity_id        VARCHAR(500),
    metadata_url     VARCHAR(500),
    client_id        VARCHAR(255),
    client_secret    VARCHAR(500),
    issuer           VARCHAR(500),
    enabled          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE approval_chains (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    name             VARCHAR(120) NOT NULL,
    description      TEXT,
    min_amount       NUMERIC(18,2),
    max_amount       NUMERIC(18,2),
    steps            JSONB NOT NULL DEFAULT '[]',  -- [{role, user_id, timeout_hours}]
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_approval_chains_org ON approval_chains(organization_id);

CREATE TABLE approval_requests (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    invoice_id       UUID NOT NULL REFERENCES invoices(id),
    chain_id         UUID NOT NULL REFERENCES approval_chains(id),
    current_step     INT NOT NULL DEFAULT 0,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED, ESCALATED, EXPIRED
    steps_state      JSONB NOT NULL DEFAULT '[]',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_approval_requests_org     ON approval_requests(organization_id, status);
CREATE INDEX idx_approval_requests_invoice ON approval_requests(invoice_id);

CREATE TABLE data_retention_policies (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL UNIQUE,
    invoice_retention_days INT NOT NULL DEFAULT 2555,  -- 7 years
    audit_retention_days   INT NOT NULL DEFAULT 2555,
    storage_retention_days INT NOT NULL DEFAULT 2555,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
