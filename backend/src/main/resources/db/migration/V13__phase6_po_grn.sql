-- Phase 6: PO/GRN Matching

CREATE TABLE purchase_orders (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    po_number       VARCHAR(100) NOT NULL,
    supplier_gstin  VARCHAR(20),
    supplier_name   VARCHAR(250),
    po_date         DATE,
    currency        VARCHAR(10) NOT NULL DEFAULT 'INR',
    total_amount    NUMERIC(19,2) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',  -- OPEN, PARTIALLY_MATCHED, MATCHED, CLOSED
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(organization_id, po_number)
);

CREATE TABLE po_lines (
    id              UUID PRIMARY KEY,
    po_id           UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    line_number     INTEGER NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    hsn_sac         VARCHAR(20),
    quantity        NUMERIC(19,4) NOT NULL,
    unit_price      NUMERIC(19,2) NOT NULL,
    tax_rate        NUMERIC(7,4),
    line_total      NUMERIC(19,2) NOT NULL
);

CREATE TABLE goods_receipts (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    po_id           UUID REFERENCES purchase_orders(id) ON DELETE SET NULL,
    grn_number      VARCHAR(100) NOT NULL,
    receipt_date    DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',  -- RECEIVED, PARTIALLY_MATCHED, MATCHED
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(organization_id, grn_number)
);

CREATE TABLE grn_lines (
    id              UUID PRIMARY KEY,
    grn_id          UUID NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    po_line_id      UUID REFERENCES po_lines(id) ON DELETE SET NULL,
    line_number     INTEGER NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    hsn_sac         VARCHAR(20),
    quantity_received NUMERIC(19,4) NOT NULL,
    unit_price      NUMERIC(19,2)
);

CREATE TABLE invoice_po_matches (
    id              UUID PRIMARY KEY,
    invoice_id      UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    po_id           UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    grn_id          UUID REFERENCES goods_receipts(id) ON DELETE SET NULL,
    match_type      VARCHAR(20) NOT NULL,  -- TWO_WAY, THREE_WAY
    match_status    VARCHAR(30) NOT NULL,  -- MATCHED, PARTIAL, OVER_BILLED, UNDER_BILLED, UNMATCHED
    discrepancies   JSONB,                 -- array of {field, poValue, invoiceValue, grnValue, message}
    matched_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    matched_by      VARCHAR(100)
);

CREATE INDEX idx_po_org           ON purchase_orders(organization_id);
CREATE INDEX idx_po_supplier      ON purchase_orders(supplier_gstin);
CREATE INDEX idx_po_status        ON purchase_orders(status);
CREATE INDEX idx_grn_org          ON goods_receipts(organization_id);
CREATE INDEX idx_grn_po           ON goods_receipts(po_id);
CREATE INDEX idx_ipm_invoice      ON invoice_po_matches(invoice_id);
CREATE INDEX idx_ipm_po           ON invoice_po_matches(po_id);
