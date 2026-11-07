CREATE TABLE subscriptions (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(80) NOT NULL,
  customer_ref VARCHAR(120) NOT NULL,
  plan_id UUID NOT NULL REFERENCES billing_plans(id),
  status VARCHAR(20) NOT NULL CHECK(status IN ('ACTIVE','PAUSED','CANCELED')),
  started_at TIMESTAMPTZ NOT NULL,
  ended_at TIMESTAMPTZ,
  UNIQUE(tenant_id,customer_ref)
);
CREATE INDEX idx_subscriptions_tenant_status ON subscriptions(tenant_id,status);

