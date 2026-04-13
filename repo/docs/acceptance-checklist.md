# Acceptance checklist (hard-fail rules)

**Delivery progress:** [`implementation-status.md`](implementation-status.md).

Mapped to [`plans/implementation_plan.md`](../../plans/implementation_plan.md) §13.

| Rule | Implementation actions | Release gate |
|------|-------------------------|--------------|
| Docker startup | Healthchecks, `depends_on` with healthy DB, Flyway on backend start, pinned base images | Clean `docker compose up --build`; review logs |
| No env-specific deps | No absolute host paths; env only via Compose and `.env.example` | Review + grep for path leaks |
| README parity | Ports, URLs, API prefix, seed credentials, health URL, `./run_test.sh` | Manual verification each release |
| Core requirements | Traceability vs product prompt | Sign-off per major milestone |
| No fake production paths | Services use PostgreSQL; seeds only via migrations | Code review + DB tests |
| Complete structure | `backend/`, `frontend/`, migrations, tests, `run_test.sh`, Compose | Checklist audit |

**Phase 0–1 gate:** Repository layout exists; Compose builds and starts all services; backend health returns UP; Flyway applies auth migrations in CI (Testcontainers) and in Compose; seed roles and admin user present; aggregate `./run_test.sh` runs backend and frontend tests.

**Phase 2 gate:** JWT login/me/logout; password policy on API (≥8 chars); `login_audit` populated; RBAC enforced server-side; Angular login + guards; `./run_test.sh` green.

**Phase 3 gate:** Canonical JSON import via `POST /api/v1/admin/imports/run` (ADMIN); `GET /api/v1/admin/imports` lists jobs; `GET /api/v1/routes`, `GET /api/v1/routes/{id}` (authenticated); sample payload in `samples/`; `TransitCanonicalImportIT` green.

**Phase 4 gate:** `GET /api/v1/search/suggestions`, `GET /api/v1/search/results` with query validation, ranking config, and popularity tracking; `GET /api/v1/stops/{id}`; `SearchApiIT` green.

**Phase 5 gate:** `POST/GET /api/v1/passenger/reservations`, `PUT .../reservations/{id}` (PASSENGER/ADMIN); `POST/GET /api/v1/passenger/checkins`; `GET/PUT /api/v1/passenger/reminder-preferences`; DND windows table; `GET/POST /api/v1/messages`, `GET .../messages/{id}`, `POST .../messages/{id}/read`; message queue with delivery attempts; redaction rules; reservation/cancel auto-generates queued notifications; dispatcher role excluded from passenger endpoints; `PassengerMessagingIT` green.

**Phase 6 gate:** `POST/GET /api/v1/workflows`, `GET /api/v1/workflows/{instanceId}` (ADMIN/DISPATCHER); `POST/GET /api/v1/tasks`, `POST .../tasks/{id}/approve|reject|return`, `POST /api/v1/tasks/batch`; 3 seeded workflow definitions; auto instance status rollup on task decisions; double-decision returns 409; passenger role excluded; `WorkflowIT` green.

**Phase 7 gate:** `GET/POST/PUT/DELETE /api/v1/admin/cleaning-rules`; `GET/POST/PUT/DELETE /api/v1/admin/dictionaries`; `GET/PUT /api/v1/admin/ranking-config`; `GET /api/v1/admin/templates`; `GET /api/v1/admin/users`, `GET/PUT .../users/{id}` (enable/disable); `GET /api/v1/admin/audit`; all ADMIN-only; non-admin returns 403; `AdminConsoleIT` green.

**Phase 8 gate:** `GET/POST /api/v1/admin/alerts`, `POST .../alerts/{id}/acknowledge` (ADMIN); `GET/POST /api/v1/admin/diagnostics` with DB_HEALTH, TABLE_STATS, CONNECTION_POOL, FULL report types; Actuator exposes `health`, `info`, `metrics`, `flyway`, `env`; health shows db/diskspace components for authorized users; `X-Trace-Id` on all responses; `ObservabilityIT` green.

**Phase 9 gate:** `scripts/backup-db.sh` + `.ps1` produce SQL dump from running Compose postgres; `scripts/restore-db.sh` + `.ps1` restore from dump; graceful shutdown configured; Hikari pool tuned with leak detection; backend healthcheck in Compose; frontend depends on backend health; `stop_grace_period` set.

**Phase 10 gate:** All three frontend role pages (passenger, dispatcher, admin) provide functional UI wired to every backend API endpoint; API path alignment verified against backend controllers; Angular production build succeeds with zero errors; tab-based navigation covers all feature areas; role-based navigation links adapt per user role; `docker compose up --build` serves the complete application stack.
