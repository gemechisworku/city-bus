# Implementation status

Living record of delivery against [`plans/implementation_plan.md`](../../plans/implementation_plan.md) (§12). Update this file when a phase is closed.

| Phase | Name | Status | Notes |
|-------|------|--------|--------|
| **0** | Inception | **Complete** | ADR (`docs/adr/`), API outline, ERD reference, acceptance checklist, `samples/integration/` placeholder, Docker/Compose baseline |
| **1** | Foundation and infrastructure | **Complete** | Angular 19 workspace, Spring Boot 3.4 + Flyway (`V1` auth schema, `V2` roles + admin seed), Dockerfiles, `docker-compose.yml`, Actuator health, `TraceIdFilter` / `X-Trace-Id`, `GET /api/v1/ping`, CI (`repo/.github/workflows/ci.yml`), `run_test.sh` (Maven verify + Testcontainers, frontend Karma) |
| **2** | Authentication and RBAC | Not started | Login/logout/me, password policy, guards, audit API |
| **3** | Bus data ingestion and canonical model | Not started | |
| **4** | Search engine and ranking | Not started | |
| **5** | Passenger reservation, check-in, reminders, message center | Not started | |
| **6** | Dispatcher workflow platform | Not started | |
| **7** | Administrator configuration console | Not started | |
| **8** | Observability, alerts, diagnostics | Not started | (Phase 1 includes basic health, logging pattern, trace header only.) |
| **9** | Backup, restore, operational hardening | Not started | |
| **10** | Final QA and rejection-proofing | Not started | |

## Plan checklist (§18) crosswalk

| §18 item | State |
|----------|--------|
| Repo skeleton: Compose, backend, frontend, samples, `.env.example`, `run_test.sh`, README | Done |
| `docker compose up --build` clean; no host-only paths | Done (verify when changing Compose) |
| Auth + seeds + README credentials | **Partial:** DB auth tables + BCrypt seed + README; **no** login/session API yet (Phase 2) |
| Ingestion + canonical data + tests | Not started |
| Search + ranking + tests | Not started |
| Reservations, reminders, DND, queue, message center + tests | Not started |
| Workflow + dispatcher + tests | Not started |
| Admin CRUD + tests | Not started |
| Observability + diagnostics + tests | Partial (foundation only) |
| Backup/restore documented + scripts | Not started |
| Phase 10 QA | Not started |

**Last updated:** 2026-04-13
