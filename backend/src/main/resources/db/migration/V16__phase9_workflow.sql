-- Phase 9: Configurable workflow engine
CREATE TABLE workflow_rules (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    name             VARCHAR(200) NOT NULL,
    priority         INTEGER NOT NULL DEFAULT 100,
    conditions       JSONB NOT NULL DEFAULT '[]',
    action           VARCHAR(40) NOT NULL,
    required_role    VARCHAR(40),
    next_state       VARCHAR(40) NOT NULL,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_workflow_rules_org_prio ON workflow_rules(organization_id, priority) WHERE active = TRUE;
