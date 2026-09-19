-- Phase 4 foundations: vendors table + invoice columns + audit event enrichment

CREATE TABLE vendors (
    id                  UUID PRIMARY KEY,
    gstin               VARCHAR(20)   NOT NULL UNIQUE,
    normalized_name     VARCHAR(250)  NOT NULL,
    total_invoice_count INTEGER       NOT NULL DEFAULT 0,
    total_invoice_value NUMERIC(19,2) NOT NULL DEFAULT 0,
    first_seen_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_vendors_gstin ON vendors(gstin);

ALTER TABLE invoices
    ADD COLUMN document_hash         VARCHAR(64),
    ADD COLUMN arithmetic_status     VARCHAR(20),
    ADD COLUMN duplicate_score       INTEGER,
    ADD COLUMN duplicate_invoice_id  UUID REFERENCES invoices(id) ON DELETE SET NULL,
    ADD COLUMN supplier_gstin_status VARCHAR(20),
    ADD COLUMN customer_gstin_status VARCHAR(20),
    ADD COLUMN vendor_id             UUID REFERENCES vendors(id);

CREATE INDEX idx_invoices_document_hash  ON invoices(document_hash);
CREATE INDEX idx_invoices_supplier_gstin ON invoices(supplier_gstin);
CREATE INDEX idx_invoices_invoice_number ON invoices(invoice_number);

ALTER TABLE invoice_events
    ADD COLUMN actor    VARCHAR(100),
    ADD COLUMN metadata JSONB;
