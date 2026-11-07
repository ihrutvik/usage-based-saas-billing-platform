CREATE TABLE credit_notes (
  id UUID PRIMARY KEY,
  credit_number VARCHAR(40) NOT NULL UNIQUE,
  tenant_id VARCHAR(80) NOT NULL,
  invoice_id UUID NOT NULL REFERENCES invoices(id),
  currency CHAR(3) NOT NULL,
  amount NUMERIC(19,2) NOT NULL CHECK(amount > 0),
  reason VARCHAR(500) NOT NULL,
  idempotency_key VARCHAR(120) NOT NULL,
  request_fingerprint CHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  UNIQUE(tenant_id,idempotency_key)
);
CREATE INDEX idx_credit_notes_invoice ON credit_notes(invoice_id,created_at);
