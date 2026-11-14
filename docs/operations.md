# Operations and SLO guide

## Service objectives

| Signal | Objective | Alert |
| --- | --- | --- |
| API availability | At least 99% non-5xx responses over the operational window | `BillingApiAvailabilityBurn` at more than 1% 5xx for 5 minutes |
| Usage ingestion | At least 99.5% non-5xx responses | `UsageIngestionUnavailable` at more than 0.5% 5xx for 5 minutes |
| API latency | p95 below 500 ms | `BillingApiLatencyHigh` for 10 minutes |
| Scrape health | Application continuously observable | `BillingServiceDown` after 2 minutes |
| Redis projection | No failed or degraded writes | Immediate failure/degraded alerts |
| JVM heap | Sustained use below 85% | `BillingJvmHeapPressure` for 10 minutes |

These are portfolio defaults, not universal targets. A production team should derive objectives from customer commitments and measure multi-window burn rates against an explicit error budget.

## First-response playbooks

### Elevated API errors

1. Use the Grafana HTTP-outcome panel to separate one endpoint from a service-wide failure.
2. Follow a failing response's `X-Correlation-Id` into application logs, then use its trace ID in Jaeger.
3. Check RDS connection pressure, Kafka reachability, Redis health, and the latest deployment event.
4. Roll back through the ECS deployment circuit breaker if the errors began with a release.

### Redis projection failure

PostgreSQL remains authoritative. Keep the Kafka consumer fail-closed so delivery retries cannot lose projection updates. Restore Redis, verify the consumer catches up, and compare current-period counters with ledger aggregation. If `REALTIME_USAGE_FAIL_OPEN=true` was intentionally enabled, rebuild the affected month from the immutable usage ledger before trusting the cache.

### Kafka or outbox delay

Ingestion can continue because the usage row and outbox event commit together. Restore broker connectivity and allow expired leases to be reclaimed. Do not manually mark events published. Validate the stable event IDs and consumer inbox before considering any replay.

### Billing dispute

Treat the immutable usage event, invoice lines, and captured price tiers as the audit chain. Never edit a finalized invoice. Resolve overcharges with a bounded credit note; route late usage through the adjustment workflow.

## Capacity and load validation

`load-tests/usage-ingestion.js` exercises the most correctness-sensitive write path under concurrency. It also submits each payload twice to verify exact replay behavior. Its thresholds are a regression gate, not a capacity claim: production sizing requires running the scenario against a representative environment, dataset, network path, and observability configuration.
