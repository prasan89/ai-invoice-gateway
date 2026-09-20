-- Phase 16: AI Anomaly Detection

CREATE TABLE invoice_anomalies (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    invoice_id       UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    risk_score       INT NOT NULL DEFAULT 0,       -- 0-100
    risk_level       VARCHAR(10) NOT NULL DEFAULT 'LOW',  -- LOW, MEDIUM, HIGH, CRITICAL
    anomaly_types    JSONB NOT NULL DEFAULT '[]',  -- ["AMOUNT_SPIKE","GST_MISMATCH","NEW_VENDOR",...]
    reasons          JSONB NOT NULL DEFAULT '[]',  -- human-readable explanations
    vendor_baseline  JSONB,                        -- snapshot of vendor stats at detection time
    detected_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    reviewed_by      UUID,
    reviewed_at      TIMESTAMP WITH TIME ZONE,
    review_outcome   VARCHAR(20),                  -- CONFIRMED, FALSE_POSITIVE, ESCALATED
    UNIQUE(invoice_id)
);

CREATE INDEX idx_anomalies_org      ON invoice_anomalies(organization_id, detected_at DESC);
CREATE INDEX idx_anomalies_risk     ON invoice_anomalies(organization_id, risk_level, detected_at DESC);
CREATE INDEX idx_anomalies_invoice  ON invoice_anomalies(invoice_id);

-- Vendor behavior baseline (updated incrementally)
CREATE TABLE vendor_baselines (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    supplier_gstin   VARCHAR(20) NOT NULL,
    supplier_name    VARCHAR(255),
    invoice_count    INT NOT NULL DEFAULT 0,
    avg_amount       NUMERIC(18,2),
    stddev_amount    NUMERIC(18,2),
    min_amount       NUMERIC(18,2),
    max_amount       NUMERIC(18,2),
    typical_gst_rate NUMERIC(6,2),
    first_seen       TIMESTAMP WITH TIME ZONE,
    last_seen        TIMESTAMP WITH TIME ZONE,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(organization_id, supplier_gstin)
);

CREATE INDEX idx_vendor_baselines_org ON vendor_baselines(organization_id);

-- Risk score on invoice (add column)
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS risk_score  INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS risk_level  VARCHAR(10) DEFAULT 'LOW';
