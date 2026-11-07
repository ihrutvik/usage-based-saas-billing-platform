-- Existing pre-V7 ledger rows remain readable but cannot be safely attributed when a
-- tenant owns multiple subscriptions. New ingestion always supplies this value.
ALTER TABLE subscriptions ADD CONSTRAINT uq_subscriptions_id_tenant UNIQUE(id, tenant_id);
ALTER TABLE usage_events ADD COLUMN subscription_id UUID;
ALTER TABLE usage_events ADD CONSTRAINT fk_usage_subscription_tenant
    FOREIGN KEY(subscription_id, tenant_id) REFERENCES subscriptions(id, tenant_id);
CREATE INDEX idx_usage_events_subscription_time
    ON usage_events(tenant_id, subscription_id, occurred_at);

-- A new projection avoids guessing attribution for legacy tenant-only buckets.
-- It can be rebuilt deterministically from subscription-attributed ledger events.
CREATE TABLE subscription_usage_hourly_buckets (
  tenant_id VARCHAR(80) NOT NULL,
  subscription_id UUID NOT NULL,
  meter_key VARCHAR(80) NOT NULL,
  bucket_start TIMESTAMPTZ NOT NULL,
  quantity NUMERIC(24,8) NOT NULL CHECK(quantity >= 0),
  event_count BIGINT NOT NULL CHECK(event_count >= 0),
  updated_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY(tenant_id, subscription_id, meter_key, bucket_start),
  FOREIGN KEY(subscription_id, tenant_id) REFERENCES subscriptions(id, tenant_id)
);
CREATE INDEX idx_subscription_usage_period
    ON subscription_usage_hourly_buckets(subscription_id, meter_key, bucket_start);
