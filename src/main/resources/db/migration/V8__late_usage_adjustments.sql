CREATE TABLE late_usage_adjustments (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(80) NOT NULL,
  subscription_id UUID NOT NULL,
  invoice_id UUID NOT NULL REFERENCES invoices(id),
  usage_event_id UUID NOT NULL REFERENCES usage_events(id),
  meter_key VARCHAR(80) NOT NULL,
  quantity NUMERIC(24,8) NOT NULL CHECK(quantity > 0),
  occurred_at TIMESTAMPTZ NOT NULL,
  status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING','INVOICED','WAIVED')),
  detected_at TIMESTAMPTZ NOT NULL,
  resolved_at TIMESTAMPTZ,
  resolution_reason VARCHAR(500),
  UNIQUE(usage_event_id),
  FOREIGN KEY(subscription_id, tenant_id) REFERENCES subscriptions(id, tenant_id)
);
CREATE INDEX idx_late_adjustments_review_queue
  ON late_usage_adjustments(tenant_id, status, detected_at);
CREATE INDEX idx_late_adjustments_subscription
  ON late_usage_adjustments(subscription_id, occurred_at);
