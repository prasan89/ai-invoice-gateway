-- Phase 5: Production Invoice Intelligence

-- 5.1: GST verification cache (avoid hammering the portal on every invoice)
CREATE TABLE gstin_verifications (
    gstin               VARCHAR(20)   PRIMARY KEY,
    legal_name          VARCHAR(300),
    trade_name          VARCHAR(300),
    registration_status VARCHAR(30),   -- ACTIVE, CANCELLED, SUSPENDED, UNKNOWN
    state_code          VARCHAR(2),
    business_nature     VARCHAR(200),
    verified_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    raw_response        JSONB
);

-- 5.1: Portal verification result cached on invoice for fast retrieval
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS supplier_legal_name    VARCHAR(300),
    ADD COLUMN IF NOT EXISTS supplier_portal_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS supplier_trade_name    VARCHAR(300);

-- 5.2: Duplicate reason string on invoices
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS duplicate_reason VARCHAR(200);

-- 5.3: Supplier intelligence columns on vendors
ALTER TABLE vendors
    ADD COLUMN IF NOT EXISTS typical_gst_rate     NUMERIC(7,4),
    ADD COLUMN IF NOT EXISTS typical_payment_days INTEGER,
    ADD COLUMN IF NOT EXISTS anomaly_count        INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_anomaly_at      TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS risk_tier            VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- Phase 6: PO/GRN match result on invoice
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS po_match_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS matched_po_id   UUID;

CREATE INDEX idx_gstin_verifications_verified ON gstin_verifications(verified_at);
