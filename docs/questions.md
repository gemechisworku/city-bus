# Prerequisite questions

1. What authoritative sources and update cadence will supply bus routes, stops, and schedules (including sample HTML/JSON templates or file locations)?
   - **Assumption:** Initial development will use representative sample HTML/JSON files checked into the repo and a daily batch import schedule unless stakeholders specify otherwise.
   - **Solution:** Implemented an admin-only `POST /api/v1/admin/imports/run` endpoint that accepts canonical JSON payloads validated against registered `field_mappings` templates (e.g. `DEFAULT_V1`), persisting versioned routes, stops, and schedules with import job history in `source_import_jobs`.

2. Will arrival reminders rely on real-time vehicle positions, static timetables only, or a defined mix, and how is latency or GPS accuracy bounded?
   - **Assumption:** Reminders will combine scheduled timetables with optional real-time offsets when feed data exists, with a conservative default of timetable-only if no live feed is configured.
   - **Solution:** Reminders are timetable-only today—passenger reminder preferences (`enabled`, `minutesBefore`, `channel`) and do-not-disturb windows are persisted via REST, and reservation lifecycle events generate in-app messages from static schedule data with no real-time vehicle feed integrated yet.

3. What do "reservation" and "check-in" mean operationally (e.g., seat booking, stop bookmark, staff attendance), and who defines the business rules?
   - **Assumption:** "Reservation" means a passenger-selected stop/time intent and "check-in" means confirming presence at the stop within a grace window, with product owning rule changes until a formal owner is named.
   - **Solution:** A reservation links a passenger to a specific schedule + stop with a `PENDING → CONFIRMED | CANCELLED` lifecycle, while check-in records the passenger's presence at a stop with an optional reservation link, both exposed via `/api/v1/passenger/reservations` and `/api/v1/passenger/checkins`.

4. Should passengers receive in-app notifications via browser push, polling, WebSockets, or another mechanism available on the offline LAN?
   - **Assumption:** The passenger UI will poll the message center on a short interval plus optional WebSockets when the LAN supports sticky sessions and reverse proxies.
   - **Solution:** Notifications are delivered as in-app messages via a REST message center (`GET /api/v1/messages`), with the passenger UI refreshing on user actions; no WebSocket or polling timer is wired yet, and a DB-backed `message_queue` table is prepared for future async delivery.

5. Will dispatchers and administrators use the same Angular app (role-based), separate Angular apps, or a non-Angular console?
   - **Assumption:** One Angular workspace will serve passengers, dispatchers, and administrators behind route guards and feature flags to reduce duplication.
   - **Solution:** A single Angular 19 standalone-component app serves all three roles via lazy-loaded feature routes (`/passenger`, `/dispatcher`, `/admin`) protected by an `authGuard` and a `roleGuard` that checks JWT-embedded roles.

6. Which workflow engine or approach is mandated or preferred for approvals (e.g., embedded engine, Camunda, Flowable, custom), and what are licensing constraints?
   - **Assumption:** We will start with a Spring-managed state machine and persisted task tables compatible with later migration to Flowable/Camunda if license review requires it.
   - **Solution:** Approvals use a custom Spring service (`WorkflowService`) with persisted `workflow_instances` and `workflow_tasks` tables supporting approve/reject/return decisions and automatic instance-status rollup, with no external engine dependency.

7. What are the target versions for Angular, Spring Boot, Java, and PostgreSQL, and are any corporate standard stacks fixed?
   - **Assumption:** The baseline will be the current LTS or latest stable Angular, Spring Boot 3.x on Java 17+, and PostgreSQL 15+ unless the organization publishes a pinned list.
   - **Solution:** The project uses Angular 19.2, Spring Boot 3.4.1 on Java 17 (Temurin), and PostgreSQL 16 (`postgres:16-alpine`), with Node 22 for frontend builds.

8. How will users and roles be provisioned, reset, and audited (manual admin, LDAP later, bulk import)?
   - **Assumption:** Phase one will rely on administrator-created local accounts with salted passwords, exportable audit logs, and a documented path to LDAP later.
   - **Solution:** Local accounts with hashed passwords are managed by admin APIs, authenticated via stateless JWT, with every login attempt (success or failure) recorded in a `login_audit` table including IP and user-agent.

9. What are the expected peak concurrent users, API throughput, and PostgreSQL data volume to size hardware and connection pools?
   - **Assumption:** Sizing targets a mid-size city operator (hundreds of concurrent users, sub-million daily API calls, gigabyte-scale PostgreSQL) until capacity numbers are provided.
   - **Solution:** Default HikariCP connection pool settings from Spring Boot are used with Actuator `health`, `metrics`, and `env` endpoints exposed for runtime monitoring, and no explicit pool or throughput tuning has been applied yet.

10. What backup recovery point objective (RPO), recovery time objective (RTO), and retention policy apply to PostgreSQL and integration artifacts?
    - **Assumption:** Nightly full backups with WAL archiving for ≤24h RPO, ≤4h RTO for database restore, and 30-day retention unless compliance mandates stricter targets.
    - **Solution:** Shell and PowerShell scripts under `repo/scripts/` run `pg_dump` via Docker Compose to produce timestamped SQL backups, with matching restore scripts that pipe the dump back into `psql --single-transaction`.

11. How should "frequency priority" and "stop popularity" be initialized and refreshed when historical usage data is sparse or missing?
    - **Assumption:** Cold-start ranking will blend static route priority with equal-weight stops, then decay toward observed search and notification engagement as telemetry accrues.
    - **Solution:** Search ranking uses admin-configurable weights (`route_weight`, `stop_weight`, `popularity_weight`) in a `ranking_config` table, with stop popularity derived from `log1p(impression_count)` auto-incremented in `stop_popularity_metrics` each time a stop appears in search results.

12. Which sensitivity levels and desensitization rules apply to message content, and who approves the catalog?
    - **Assumption:** Three levels (public, internal, restricted) with field-level redaction patterns maintained by operations administrators and reviewed quarterly.
    - **Solution:** `MessageService` applies enabled regex patterns from a `message_redaction_rules` table to redact message bodies before persistence, while admin-managed `cleaning_rule_sets` provide additional CRUD-only rule configuration for future pipeline use.

13. Where should local alerts and diagnostic reports be delivered (log files, email on LAN, ticketing system, on-screen dashboard)?
    - **Assumption:** Alerts will surface in structured logs, Prometheus-style metrics, and an internal admin dashboard with optional SMTP to a LAN relay if available.
    - **Solution:** Alerts and diagnostics are persisted in `system_alerts` and `diagnostic_reports` tables and surfaced via admin REST APIs and the Angular admin dashboard, supplemented by Spring Boot Actuator health/metrics endpoints and MDC-based `X-Trace-Id` correlation in structured logs.

14. What character encoding, locale, and pinyin/initial-matching libraries are acceptable for search given the English UI and Chinese place names?
    - **Assumption:** UTF-8 end-to-end, ICU or OpenCC-compatible normalization, and a well-maintained pinyin library (e.g., pinyin4j or equivalent) for server-side matching.
    - **Solution:** Search uses UTF-8 storage and case-insensitive `ILIKE` matching on route/stop names and codes, but the query validation regex currently restricts input to ASCII alphanumerics so pinyin-to-hanzi bridging is not yet implemented.

15. What is the minimum browser and OS matrix the passenger and staff clients must support on the closed network?
    - **Assumption:** Last two major versions of Chromium-based browsers and Firefox on Windows 10/11 for staff, plus modern mobile WebKit for passenger kiosks unless narrowed by IT.
    - **Solution:** The Angular 19 app targets ES2022+ with Zone.js and is built for production via Nginx (`1.27-alpine`), relying on Angular's default `browserslist` which covers recent Chrome, Firefox, Edge, and Safari versions.
