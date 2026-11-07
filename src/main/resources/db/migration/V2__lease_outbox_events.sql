ALTER TABLE outbox_events ADD COLUMN leased_until TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN lease_owner VARCHAR(120);
ALTER TABLE outbox_events ADD COLUMN publish_attempts INTEGER NOT NULL DEFAULT 0;
CREATE INDEX idx_outbox_claim ON outbox_events(created_at) WHERE published_at IS NULL;
