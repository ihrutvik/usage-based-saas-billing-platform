CREATE TABLE invoices (
  id UUID PRIMARY KEY,
  invoice_number VARCHAR(40) NOT NULL UNIQUE,
  tenant_id VARCHAR(80) NOT NULL,
  subscription_id UUID NOT NULL REFERENCES subscriptions(id),
  currency CHAR(3) NOT NULL,
  period_start TIMESTAMPTZ NOT NULL,
  period_end TIMESTAMPTZ NOT NULL,
  total NUMERIC(19,2) NOT NULL CHECK(total >= 0),
  status VARCHAR(20) NOT NULL CHECK(status IN ('FINALIZED','VOID')),
  finalized_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CHECK(period_end > period_start),
  UNIQUE(subscription_id,period_start,period_end)
);
ALTER TABLE outbox_events ADD COLUMN destination VARCHAR(200);
CREATE INDEX idx_invoices_tenant_created ON invoices(tenant_id,created_at DESC);
CREATE TABLE invoice_lines (
  id UUID PRIMARY KEY,
  invoice_id UUID NOT NULL REFERENCES invoices(id),
  meter_key VARCHAR(80) NOT NULL,
  quantity NUMERIC(30,8) NOT NULL CHECK(quantity >= 0),
  amount NUMERIC(19,2) NOT NULL CHECK(amount >= 0),
  pricing_snapshot JSONB NOT NULL,
  UNIQUE(invoice_id,meter_key)
);
