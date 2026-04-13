# ADR 001: Modular monolith and database-backed internal queue

## Status

Accepted (Phase 0)

## Context

The City Bus platform targets an offline LAN with Docker-first operations, hundreds of concurrent users, and requirements for notifications, workflows, and ETL without depending on external SMS/email/push brokers.

## Decision

1. **Modular monolith:** A single Spring Boot application with clear package boundaries (auth, bus data, search, notifications, workflow, admin, observability). This reduces operational surface area and matches team traceability for a greenfield delivery.
2. **PostgreSQL-backed internal queue:** Notification and async work use dedicated tables (`message_queue`, attempts, etc.), idempotent consumers, and scheduled processors. Optional message brokers are out of scope until they can run entirely inside Compose with documented parity.

## Consequences

- **Positive:** Simpler `docker compose`, single deployment artifact, unified transactions with domain data, Flyway-managed schema.
- **Negative:** Queue throughput must be validated under load; horizontal scaling of workers requires explicit design (locking, partitioning) in later phases.
- **Migration path:** Workflow tables remain migration-friendly toward Flowable/Camunda; queue abstraction can be swapped if a broker is introduced with the same delivery guarantees.

## Links

- [`plans/implementation_plan.md`](../../plans/implementation_plan.md) §4, §7, §12
