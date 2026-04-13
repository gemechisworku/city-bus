# Implementation status

Living record of delivery against [`plans/implementation_plan.md`](../../plans/implementation_plan.md) (§12). Update this file when a phase is closed.

| Phase | Name | Status | Notes |
|-------|------|--------|--------|
| **0** | Inception | **Complete** | ADR (`docs/adr/`), API outline, ERD reference, acceptance checklist, `samples/integration/` placeholder, Docker/Compose baseline |
| **1** | Foundation and infrastructure | **Complete** | Angular workspace, Spring Boot 3.4 + Flyway, Dockerfiles, Compose, Actuator, trace ID, `run_test.sh`, Testcontainers |
| **2** | Authentication and RBAC | **Complete** | JWT (`POST /auth/login`, `GET /auth/me`, `POST /auth/logout`), BCrypt, password length ≥8 on login DTO, `login_audit` writes, RBAC demo routes + `@PreAuthorize`, integration tests; Angular login, HTTP Bearer interceptor, `authGuard` + `roleGuard`, role home routes |
| **3** | Bus data ingestion and canonical model | **Complete** | Flyway `V4__transit_canonical.sql` (routes, versions, stops, route_stops, schedules, `source_import_jobs`, `field_mappings` + DEFAULT_V1 mapping seed); canonical JSON import `POST /api/v1/admin/imports/run` (ADMIN); `GET /api/v1/admin/imports`; read `GET /api/v1/routes`, `GET /api/v1/routes/{id}` (authenticated); sample `samples/integration/canonical-routes.sample.json`; `TransitCanonicalImportIT` |
| **4** | Search engine and ranking | **Complete** | Flyway `V5__search_ranking.sql` (`ranking_config` DEFAULT row, `stop_popularity_metrics`, `search_events`); `GET /api/v1/search/suggestions`, `GET /api/v1/search/results` (authenticated; query `q` 2–128 safe chars; weighted text + popularity on results); impressions on `RESULTS`; `GET /api/v1/stops/{id}`; `SearchApiIT` |
| **5** | Passenger reservation, check-in, reminders, message center | **Complete** | Flyway `V6__passenger_messaging.sql` (8 tables); `POST/GET /api/v1/passenger/reservations`, `PUT .../reservations/{id}` (PASSENGER/ADMIN); `POST/GET /api/v1/passenger/checkins`; `GET/PUT /api/v1/passenger/reminder-preferences`; `GET/POST /api/v1/messages`, `GET .../messages/{id}`, `POST .../messages/{id}/read`; reservation/cancel auto-creates queued notification; redaction rules on message body; `PassengerMessagingIT` |
| **6** | Dispatcher workflow platform | **Complete** | Flyway `V7__workflow.sql` (`workflow_definitions` with 3 seed defs, `workflow_instances`, `workflow_tasks`, `workflow_escalations`); `POST/GET /api/v1/workflows`, `GET /api/v1/workflows/{instanceId}` (ADMIN/DISPATCHER); `POST/GET /api/v1/tasks`, `POST .../tasks/{id}/approve`, `.../reject`, `.../return`, `POST /api/v1/tasks/batch`; auto status rollup (OPEN→COMPLETED/REJECTED); `WorkflowIT` |
| **7** | Administrator configuration console | **Complete** | Flyway `V8__admin_config.sql` (`cleaning_rule_sets`, `cleaning_audit_logs`, `field_standard_dictionaries`); `GET/POST/PUT/DELETE /api/v1/admin/cleaning-rules`; `GET/POST/PUT/DELETE /api/v1/admin/dictionaries`; `GET/PUT /api/v1/admin/ranking-config`; `GET /api/v1/admin/templates`; `GET/PUT /api/v1/admin/users/{id}`; `GET /api/v1/admin/audit`; all ADMIN-only; `AdminConsoleIT` |
| **8** | Observability, alerts, diagnostics | **Complete** | Flyway `V9__operations.sql` (`system_alerts`, `diagnostic_reports`); `GET/POST /api/v1/admin/alerts`, `POST .../alerts/{id}/acknowledge`; `GET/POST /api/v1/admin/diagnostics` (DB_HEALTH, TABLE_STATS, CONNECTION_POOL, FULL); enhanced Actuator (`health`, `info`, `metrics`, `flyway`, `env`); health details for authorized users; `X-Trace-Id` header; `ObservabilityIT` |
| **9** | Backup, restore, operational hardening | **Complete** | `scripts/backup-db.sh` + `.ps1` (pg_dump to gzip/sql); `scripts/restore-db.sh` + `.ps1`; graceful shutdown (`server.shutdown=graceful`, 30s timeout); Hikari tuning (leak detection, idle/max-lifetime); backend healthcheck in Compose; frontend waits for backend health; `stop_grace_period` on backend |
| **10** | Final QA and rejection-proofing | **Complete** | Frontend components fully wired to backend APIs (passenger: search/reservations/check-ins/messages/reminders; dispatcher: workflows/tasks with approve/reject/return; admin: import/ranking/cleaning-rules/dictionaries/users/alerts/diagnostics/audit); all API paths verified against backend controllers; Angular production build green |

## Plan checklist (§18) crosswalk

| §18 item | State |
|----------|--------|
| Repo skeleton: Compose, backend, frontend, samples, `.env.example`, `run_test.sh`, README | Done |
| `docker compose up --build` clean; no host-only paths | Done (re-verify when changing Compose) |
| Auth + seeds + README credentials | **Done** for Phase 2 (JWT auth API + seeds + README) |
| Ingestion + canonical data + tests | Done |
| Search + ranking + tests | Done |
| Reservations, reminders, DND, queue, message center + tests | Done |
| Workflow + dispatcher + tests | Done |
| Admin CRUD + tests | Done |
| Observability + diagnostics + tests | Done |
| Backup/restore documented + scripts | Done |
| Phase 10 QA | Done |

**Last updated:** 2026-04-13 (Phase 10 complete — all phases delivered)
