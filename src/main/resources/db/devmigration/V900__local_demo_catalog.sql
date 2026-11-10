INSERT INTO billing_meters(id,tenant_id,meter_key,unit) VALUES ('10000000-0000-0000-0000-000000000001','demo-tenant','api_calls','request');
INSERT INTO billing_plans(id,tenant_id,plan_key,name,currency) VALUES ('20000000-0000-0000-0000-000000000001','demo-tenant','growth','Growth','USD');
INSERT INTO meter_prices(id,plan_id,meter_id,tier_start,tier_end,unit_price) VALUES
('30000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',0,1000,0.01000000),
('30000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',1000,10000,0.00800000),
('30000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',10000,NULL,0.00500000);
INSERT INTO subscriptions(id,tenant_id,customer_ref,plan_id,status,started_at) VALUES ('40000000-0000-0000-0000-000000000001','demo-tenant','customer-demo','20000000-0000-0000-0000-000000000001','ACTIVE','2026-11-01T00:00:00Z');
INSERT INTO api_credentials(id,key_id,secret_hash,tenant_id,display_name,scopes) VALUES
('50000000-0000-0000-0000-000000000001','demo','cd577fe2561ebff23505db0bb006300c7cdecbd46bc0e03c449afafaca2c25bf','demo-tenant','Local development',ARRAY['usage:read','usage:write','billing:read','billing:write']);
