# Repository launch kit

## GitHub description

Production-minded Java 17 usage-based billing platform with immutable metering, graduated pricing, invoices, credits, Kafka outbox/inbox, Redis projections, AWS Terraform, tracing, SLOs, and load tests.

## Recruiter summary

This project demonstrates the backend problems hidden behind usage-based SaaS pricing: tenant-safe ingestion, payload-aware idempotency, event-time aggregation, deterministic decimal rating, immutable financial records, replay-safe messaging, late-arrival policy, distributed cache correctness, and operable AWS deployment. The implementation favors explicit failure semantics and auditable state transitions over framework magic.

## LinkedIn launch post

I built a production-minded Usage-Based SaaS Billing Platform in Java 17 and Spring Boot.

The interesting part was not creating an invoice endpoint. It was designing the correctness boundaries around money and distributed events:

- immutable, tenant-scoped usage ingestion with payload-aware idempotency
- PostgreSQL transactional outbox and Kafka inbox deduplication
- event-time hourly aggregation and deterministic `BigDecimal` graduated pricing
- serialized invoice finalization, immutable price snapshots, and bounded credit notes
- explicit handling for usage that arrives after an invoice is finalized
- Redis real-time usage counters with atomic Lua deduplication
- API-key scopes, tenant derivation, Testcontainers, OpenTelemetry, and correlation IDs
- ECS Fargate, RDS, MSK Serverless, ElastiCache, Secrets Manager, autoscaling, and rollback through Terraform
- Prometheus/Grafana SLO monitoring and k6 concurrency tests

The design intentionally keeps PostgreSQL as the financial source of truth and treats Kafka and Redis as replayable delivery and projection layers. That decision makes failure recovery and billing disputes explainable.

Repository: https://github.com/ihrutvik/usage-based-saas-billing-platform

#java #springboot #backendengineering #systemdesign #kafka #postgresql #redis #aws #terraform #observability

## Interview talking points

1. Why end-to-end exactly-once is not promised, and how stable event IDs plus durable inboxes provide effective idempotency.
2. Why finalized invoices are immutable and corrections become credit notes or adjustment records.
3. How event time, receipt time, and invoice finalization time interact for late usage.
4. Why the platform begins as a modular monolith and where ingestion, metering, and invoicing can split later.
5. How the AWS topology protects data services while balancing availability and portfolio cost.
