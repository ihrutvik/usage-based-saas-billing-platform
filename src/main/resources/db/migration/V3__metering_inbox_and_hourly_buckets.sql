CREATE TABLE metering_inbox (
  event_id UUID PRIMARY KEY,
  source_topic VARCHAR(200) NOT NULL,
  source_partition INTEGER NOT NULL,
  source_offset BIGINT NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL,
  UNIQUE(source_topic, source_partition, source_offset)
);

CREATE TABLE usage_hourly_buckets (
  tenant_id VARCHAR(80) NOT NULL,
  meter_key VARCHAR(80) NOT NULL,
  bucket_start TIMESTAMPTZ NOT NULL,
  quantity NUMERIC(30,8) NOT NULL,
  event_count BIGINT NOT NULL CHECK(event_count > 0),
  updated_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY(tenant_id, meter_key, bucket_start)
);

CREATE INDEX idx_usage_buckets_period ON usage_hourly_buckets(tenant_id, bucket_start, meter_key);
