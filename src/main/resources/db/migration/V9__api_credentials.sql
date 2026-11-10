CREATE TABLE api_credentials (
  id UUID PRIMARY KEY,
  key_id VARCHAR(64) NOT NULL UNIQUE,
  secret_hash CHAR(64) NOT NULL,
  tenant_id VARCHAR(80) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  scopes VARCHAR(80)[] NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_used_at TIMESTAMPTZ,
  CONSTRAINT api_credentials_secret_hash_hex CHECK (secret_hash ~ '^[0-9a-f]{64}$'),
  CONSTRAINT api_credentials_has_scope CHECK (cardinality(scopes) > 0)
);

CREATE INDEX idx_api_credentials_tenant_active ON api_credentials(tenant_id) WHERE active;
