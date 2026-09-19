-- Phase 7: Email invoice automation logs
CREATE TABLE email_poll_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID          NOT NULL,
    polled_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    mailbox_user    VARCHAR(200),
    messages_found  INTEGER NOT NULL DEFAULT 0,
    invoices_created INTEGER NOT NULL DEFAULT 0,
    errors          INTEGER NOT NULL DEFAULT 0,
    error_detail    TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'  -- SUCCESS, PARTIAL, FAILED
);

CREATE INDEX idx_email_poll_org ON email_poll_logs (organization_id, polled_at DESC);
