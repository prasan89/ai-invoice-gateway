-- Phase 10: ERP sync jobs
CREATE TABLE erp_sync_jobs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id    UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    erp_system    VARCHAR(40) NOT NULL,
    sync_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    synced_at     TIMESTAMP WITH TIME ZONE,
    response      JSONB,
    error_detail  TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_erp_sync_invoice ON erp_sync_jobs(invoice_id);
