-- Phase 15: Billing & SaaS

CREATE TABLE subscription_plans (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(40) NOT NULL UNIQUE,  -- STARTER, GROWTH, BUSINESS, ENTERPRISE
    display_name VARCHAR(80) NOT NULL,
    monthly_price_paise BIGINT NOT NULL DEFAULT 0,
    invoice_limit INT NOT NULL DEFAULT 100,    -- per billing period; -1 = unlimited
    api_key_limit INT NOT NULL DEFAULT 1,
    user_limit    INT NOT NULL DEFAULT 2,
    features      JSONB NOT NULL DEFAULT '[]',
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

INSERT INTO subscription_plans (id, name, display_name, monthly_price_paise, invoice_limit, api_key_limit, user_limit, features) VALUES
  (gen_random_uuid(), 'STARTER',    'Starter',    0,         100,   1,  2,  '["invoice_upload","gst_validation","basic_dashboard"]'),
  (gen_random_uuid(), 'GROWTH',     'Growth',     299900,    1000,  3,  10, '["invoice_upload","gst_validation","basic_dashboard","anomaly_detection","bulk_upload","email_import","webhooks"]'),
  (gen_random_uuid(), 'BUSINESS',   'Business',   999900,    10000, 10, 50, '["invoice_upload","gst_validation","basic_dashboard","anomaly_detection","bulk_upload","email_import","webhooks","copilot","erp_integrations","po_matching","workflow_engine"]'),
  (gen_random_uuid(), 'ENTERPRISE', 'Enterprise', -1,        -1,    -1, -1, '["invoice_upload","gst_validation","basic_dashboard","anomaly_detection","bulk_upload","email_import","webhooks","copilot","erp_integrations","po_matching","workflow_engine","sso","advanced_audit","sla","data_retention"]');

CREATE TABLE organization_subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL UNIQUE,
    plan_id             UUID NOT NULL REFERENCES subscription_plans(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'TRIALING',  -- TRIALING, ACTIVE, PAST_DUE, CANCELLED, EXPIRED
    razorpay_sub_id     VARCHAR(120),
    razorpay_customer_id VARCHAR(120),
    current_period_start TIMESTAMP WITH TIME ZONE,
    current_period_end   TIMESTAMP WITH TIME ZONE,
    trial_end            TIMESTAMP WITH TIME ZONE,
    cancelled_at         TIMESTAMP WITH TIME ZONE,
    invoice_count_current INT NOT NULL DEFAULT 0,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_org_sub_org ON organization_subscriptions(organization_id);
CREATE INDEX idx_org_sub_razorpay ON organization_subscriptions(razorpay_sub_id) WHERE razorpay_sub_id IS NOT NULL;

CREATE TABLE payment_events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    subscription_id  UUID REFERENCES organization_subscriptions(id),
    event_type       VARCHAR(60) NOT NULL,  -- subscription.charged, payment.failed, subscription.cancelled, etc.
    razorpay_event_id VARCHAR(120) UNIQUE,
    amount_paise     BIGINT,
    currency         VARCHAR(10) DEFAULT 'INR',
    status           VARCHAR(20),
    payload          JSONB,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_events_org ON payment_events(organization_id, created_at DESC);

CREATE TABLE usage_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    invoice_count   INT NOT NULL DEFAULT 0,
    api_calls       BIGINT NOT NULL DEFAULT 0,
    ai_extractions  INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(organization_id, period_start)
);

CREATE INDEX idx_usage_records_org ON usage_records(organization_id, period_start DESC);

-- Seed trial subscription for the demo organization
INSERT INTO organization_subscriptions (
    id, organization_id, plan_id, status, trial_end,
    current_period_start, current_period_end
)
SELECT
    gen_random_uuid(),
    o.id,
    p.id,
    'TRIALING',
    now() + interval '14 days',
    now(),
    now() + interval '30 days'
FROM (SELECT DISTINCT organization_id AS id FROM users LIMIT 1) o,
     (SELECT id FROM subscription_plans WHERE name='STARTER') p
WHERE EXISTS (SELECT 1 FROM users);
