# Architecture decisions

## Day-one system boundary

The platform accepts immutable, tenant-scoped usage facts. PostgreSQL is the system of record; a transactional outbox records the corresponding `USAGE_RECORDED` event in the same transaction. Kafka publication, aggregation, rating, invoice lifecycle, credits, and entitlement enforcement are intentionally separate stages so ingestion remains low-latency and auditable.

## Decisions

1. **Append-only ledger.** Corrections will be new reversal/adjustment events, never destructive updates.
2. **Subscription attribution.** Every new usage fact names an active tenant-owned subscription. The identifier is part of the idempotency fingerprint and remains attached through aggregation, rating, and invoicing.
3. **Payload-aware idempotency.** `(tenant_id, external_event_id)` is unique. A canonical SHA-256 fingerprint distinguishes an exact retry from conflicting key reuse.
4. **Event-time semantics.** `occurredAt` drives billing periods; `receivedAt` supports operational auditing. A bounded late-arrival window limits reopened periods.
5. **Catalog before ingestion.** Only an active tenant meter accepts usage, preventing unpriceable data from silently accumulating.
6. **Decimal quantities and prices.** PostgreSQL `NUMERIC` and Java `BigDecimal` avoid binary floating-point billing errors.
7. **Transactional outbox.** Ledger and integration event commit atomically. The relay will use `FOR UPDATE SKIP LOCKED` for horizontal scale.
8. **Tenant header is temporary.** `X-Tenant-Id` makes the first checkpoint runnable; a later security milestone will derive tenancy from hashed API credentials and enforce scopes.

## Reliable publication

Relay replicas claim unpublished rows in small batches with `FOR UPDATE SKIP LOCKED`, then persist a time-bounded lease before releasing the database transaction. Kafka network I/O therefore never holds row locks. A successful send marks the row published in a separate short transaction; failures release the lease immediately, while process crashes recover after lease expiry.

Kafka uses `tenantId:usageEventId` as the record key so events for one ledger item remain ordered. `event-id`, `event-type`, and `tenant-id` headers provide a durable consumer contract. Delivery is intentionally at least once: consumers must deduplicate by the globally unique event ID. This avoids the false promise of end-to-end exactly-once behavior across PostgreSQL and arbitrary consumers.

## Replay-safe metering

The metering consumer inserts the outbox event ID into a PostgreSQL inbox before updating the hourly bucket, within one transaction. Duplicate Kafka deliveries find the inbox row and become no-ops. The Kafka offset is acknowledged only after that transaction commits; a crash before commit causes a safe redelivery.

Buckets use the usage event's `occurredAt`, not consumer time, so delayed events land in the correct billing period. Atomic `INSERT ... ON CONFLICT DO UPDATE` increments allow multiple consumer partitions to update the same tenant, subscription, meter, and hour without lost updates. Raw immutable events remain authoritative; hourly buckets are rebuildable projections and never replace the ledger.

## Subscription isolation

Usage is attributed before it enters the ledger. Ingestion verifies that the requested subscription is active and owned by the tenant, and PostgreSQL uses composite `(subscription_id, tenant_id)` foreign keys to make cross-tenant attribution impossible even if an application bug bypasses validation. Subscription identity participates in the canonical request fingerprint, so moving an idempotency key between subscriptions is a conflict rather than a replay.

The original tenant-only hourly projection remains as historical schema, but all new aggregation and rating uses `subscription_usage_hourly_buckets`. This avoids inventing attribution for legacy aggregate rows. A production migration would rebuild the new projection only from ledger events with known subscription identity, then retire the legacy table after reconciliation.

## Deterministic graduated rating

Each subscription resolves to one tenant-owned active plan. A plan can price multiple meters, and every meter has contiguous graduated tiers beginning at zero with a required open-ended final tier. For graduated pricing, only units falling inside a tier are multiplied by that tier's unit price; this differs from volume pricing, where one rate applies to the full quantity.

Quantities and unit prices use `BigDecimal`/PostgreSQL `NUMERIC`. Intermediate tier calculations retain full decimal precision. Each meter line is rounded once to the plan currency's two-decimal scale using `HALF_UP`, then rounded lines are summed. Persisted invoices will snapshot pricing inputs so later catalog edits cannot rewrite financial history; the current endpoint is explicitly a non-persisted preview.

## Invoice finalization

Finalization takes a PostgreSQL row lock on the tenant-owned subscription, serializing concurrent attempts for that subscription. The database also enforces one invoice per `(subscription, period_start, period_end)`. An exact retry returns the original finalized invoice rather than recalculating or double-billing.

Invoice headers, lines, complete applied pricing tiers, rounding policy, and the `INVOICE_FINALIZED` outbox event are committed in one transaction. Finalized invoices are immutable; subsequent corrections will use explicit credit notes or adjustment invoices instead of rewriting financial history. Catalog changes therefore affect future rating only, while each invoice retains the exact commercial inputs used to produce it.

Outbox rows may carry an explicit destination. Usage events use the configured usage topic, while finalized invoices route to `billing.invoice-finalized.v1`. This prevents invoice messages from reaching the metering consumer while retaining one leased relay implementation.

## Immutable corrections and credit notes

Finalized invoices are never edited. A correction creates an immutable credit note tied to the original invoice. The service locks the invoice row, sums prior credits, and verifies the requested amount does not exceed the remaining creditable balance. Concurrent requests therefore cannot collectively over-credit an invoice.

Credit creation uses a tenant-scoped idempotency key plus a canonical SHA-256 request fingerprint. Exact retries return the original credit note, while reusing the key with a different invoice, amount, or reason returns `409`. The credit record and destination-aware `CREDIT_NOTE_ISSUED` outbox event commit atomically; downstream accounting integrations consume `billing.credit-note-issued.v1` independently of usage and invoice streams.

## Late-arriving usage adjustments

An event is late for billing when its `occurredAt` belongs to a finalized invoice period and its ledger `receivedAt` is later than that invoice's `finalizedAt`. Metering still adds the event to the subscription projection, but it never reopens or mutates the finalized invoice. Within the same inbox transaction, it creates a unique `PENDING` adjustment tied to the immutable usage event and invoice.

The uniqueness constraint on `usage_event_id` and the metering inbox make duplicate Kafka delivery harmless. The adjustment and destination-aware `LATE_USAGE_DETECTED` outbox event commit atomically. Operators can list the tenant-and-subscription-scoped queue and waive an item only after locking its pending row; the reason, resolution time, and `LATE_USAGE_WAIVED` event form an audit trail. Converting candidates into debit notes is deliberately separate so commercial policy does not leak into ingestion.

## Redis real-time usage projection

The PostgreSQL ledger and hourly buckets remain authoritative for rating and invoicing. Redis is a rebuildable, current-period read model for dashboards and usage warnings. After PostgreSQL aggregation commits, the Kafka consumer runs a Lua script that uses `SET NX EX` on the immutable event ID and `INCRBYFLOAT` on the tenant, subscription, meter, and UTC month counter. Both operations execute atomically, so broker redelivery cannot inflate usage.

The consumer acknowledges Kafka only after the Redis script succeeds. The default fail-closed policy converts a Redis failure into Kafka redelivery; because both the PostgreSQL inbox and Redis event key are idempotent, either projection can safely have succeeded before the retry. Operators may set `REALTIME_USAGE_FAIL_OPEN=true` when ingestion availability matters more than a temporary cache gap. That mode is observable through `billing.realtime_usage.events{outcome="degraded"}` and requires rebuilding affected counters from the immutable ledger.

Redis keys encode tenant and meter segments with URL-safe Base64, avoiding ambiguous delimiters while keeping the subscription and month visible. Event and counter TTLs default to 120 days and are independently configurable. The API returns `503` when Redis is unavailable rather than silently falling back to a potentially expensive database aggregation.

## Planned 14-run evolution

Kafka relay and aggregation, graduated/volume rating, subscriptions and billing periods, invoice finalization, credits, late-arrival adjustments, Redis-based real-time counters, security, Testcontainers, OpenTelemetry, AWS Terraform, load tests, and SLO dashboards.
