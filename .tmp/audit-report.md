# City Bus Platform — Delivery Acceptance & Architecture Audit

**Audit date:** 2026-04-13  
**Audit type:** Static-only (no project start, no Docker, no test execution, no code modification)

---

## 1. Verdict

**Overall conclusion: Partial Pass**

The project delivers a substantial, architecturally coherent full-stack platform covering most of the Prompt's core requirements. The codebase is well-structured with clear module boundaries, professional engineering practices, and meaningful integration tests. However, several explicitly stated Prompt requirements are missing or only superficially implemented — most notably: pinyin/initial letter search matching, Do-Not-Disturb window management endpoints, notification queue consumption (no scheduled processor), workflow timeout escalation, and cleaning rule application during data ingestion. These gaps represent material functional deficits that prevent a full pass.

---

## 2. Scope and Static Verification Boundary

### What was reviewed
- All 140 backend Java source files under `repo/backend/src/main/java/`
- All 9 Flyway migrations (`V1__auth_tables.sql` through `V9__operations.sql`)
- All 7 backend integration test files + 2 test resources
- All 26 frontend TypeScript/HTML/SCSS source files
- 1 frontend unit test file
- `pom.xml`, `application.yml`, `application-test.yml`, `docker-compose.yml`
- `.env.example`, `proxy.conf.json`, `angular.json`, `package.json`
- All documentation: `README.md`, `docs/acceptance-checklist.md`, `docs/api-outline.md`, `docs/data-model-erd.md`, `docs/implementation-status.md`, `docs/adr/001-monolith-and-db-queue.md`
- All scripts: `run_test.sh`, `mvn-verify-docker.sh`, backup/restore scripts
- CI workflow: `.github/workflows/ci.yml`

### What was NOT reviewed
- Build output under `backend/target/`
- `node_modules/` and `package-lock.json` contents
- Binary files (favicon.ico)

### What was intentionally NOT executed
- `docker compose up --build`
- `./run_test.sh` or `mvnw verify`
- `ng build` or `ng serve`
- Any database connection or container orchestration

### Claims requiring manual verification
- Docker Compose produces a clean build and all services start
- All 7 integration tests pass with Testcontainers
- Angular production build succeeds with zero errors
- Backup/restore scripts produce valid database dumps
- Seed credentials authenticate correctly at runtime

---

## 3. Repository / Requirement Mapping Summary

### Prompt core business goal
A "City Bus Operation and Service Coordination Platform" targeting three roles (passenger, dispatcher, admin) with: route/stop search with autocomplete and pinyin matching, notification preferences with DND, a message center, a dispatcher workflow engine with conditional branching and timeout escalation, admin configuration of templates/rules/dictionaries, Spring Boot on offline LAN, PostgreSQL with backup, structured bus data ingestion with cleaning, local auth with password policy, message desensitization, in-platform message queue with scheduled task triggers, and observability with structured logging, metrics, health checks, trace IDs, and threshold-based alerting.

### Implementation mapping summary

| Prompt area | Primary implementation |
|---|---|
| Passenger search | `SearchController`, `SearchService`, `SearchApiIT` |
| Autocomplete/pinyin | `SearchService` (ILIKE only — **no pinyin**) |
| Notification prefs/DND | `ReminderPreferenceEntity`, `DoNotDisturbWindowEntity` (table exists, **no API endpoint for DND**) |
| Message center | `MessageController`, `MessageService`, `PassengerMessagingIT` |
| Dispatcher workflows | `WorkflowController`, `TaskController`, `WorkflowService`, `WorkflowIT` |
| Workflow timeout/escalation | `WorkflowEscalationEntity` (table exists, **no scheduler**) |
| Admin config | `AdminConfigController`, `AdminConfigService`, `AdminConsoleIT` |
| Data ingestion | `CanonicalImportService`, `CanonicalImportTransactionalService`, `TransitCanonicalImportIT` |
| Cleaning during ingestion | `CleaningRuleSetEntity` (configurable rules exist, **not applied during import**) |
| Auth/RBAC | `AuthController`, `AuthService`, `SecurityConfig`, `JwtService` |
| Message queue | `MessageQueueEntity`, `MessageQueueAttemptEntity` (table exists, **no consumer/scheduler**) |
| Observability | `TraceIdFilter`, `OperationsService`, Actuator endpoints, `ObservabilityIT` |
| P95/queue threshold alerts | **Not implemented** (manual alert creation only) |
| Backup/restore | `scripts/backup-db.sh`, `scripts/restore-db.ps1` |

---

## 4. Section-by-section Review

### 4.1 Hard Gates

#### 4.1.1 Documentation and static verifiability

**Conclusion: Pass**

- `repo/README.md` provides clear startup instructions (`docker compose up --build`), seed credentials with passwords, test execution commands, stack versions, and URL/port table.  
  Evidence: `repo/README.md:1-130`
- `.env.example` documents all configurable environment variables.  
  Evidence: `repo/.env.example:1-13`
- `run_test.sh` aggregates backend (Docker-based Maven verify) and frontend (`npm run test:ci`) testing.  
  Evidence: `repo/run_test.sh:1-13`
- `docs/api-outline.md` provides a comprehensive API reference.
- `docs/acceptance-checklist.md` maps phases to verification gates.
- `docs/implementation-status.md` tracks all 11 phases as complete.
- Entry points (`CityBusApplication.java`, `main.ts`), configuration (`application.yml`, `angular.json`), and project structure are statically consistent.

#### 4.1.2 Material deviation from Prompt

**Conclusion: Partial Pass**

The implementation is centered on the correct business goal and covers the majority of the Prompt's requirements. However, several explicitly stated requirements are missing or partially implemented (detailed in Section 5):
- Pinyin/initial letter search matching: not implemented (`SearchService.java:28` — regex allows only `[a-zA-Z0-9\\s\\-]`, no pinyin conversion logic)
- DND window API endpoints: table exists (`V6__passenger_messaging.sql:35-42`) but no controller endpoint exposes CRUD
- Notification queue consumer/scheduler: queue tables exist but no `@Scheduled` processor
- Workflow timeout escalation: escalation table exists (`V7__workflow.sql:51-58`) but no timeout monitoring scheduler
- Cleaning rule application during import: rules are configurable but never invoked during `CanonicalImportTransactionalService.importCanonical()`
- HTML template parsing: only JSON import is supported

These are not "loosely related" additions — they are explicit Prompt requirements that are structurally present (tables, entities) but functionally incomplete (no processing logic or API exposure).

---

### 4.2 Delivery Completeness

#### 4.2.1 Core requirements coverage

**Conclusion: Partial Pass**

**Implemented and statically verifiable:**
- ✅ Three-role system (ADMIN, DISPATCHER, PASSENGER) with RBAC
- ✅ Route/stop search by code and name with weighted ranking and popularity tracking
- ✅ Configurable ranking weights (route/stop/popularity)
- ✅ Reservation creation, confirmation, cancellation with auto-notification
- ✅ Check-in tracking
- ✅ Reminder preferences (enabled, minutesBefore, channel)
- ✅ Message center with create, list, mark-read, and redaction rules
- ✅ Dispatcher workflow: instance creation, task create/approve/reject/return, batch decisions, auto-status rollup
- ✅ Admin CRUD: cleaning rules, dictionaries, ranking config, user enable/disable, audit log
- ✅ Data import from canonical JSON with versioned routes/stops/schedules
- ✅ JWT auth with BCrypt, password ≥8 chars enforced
- ✅ PostgreSQL with Flyway migrations, local backup/restore scripts
- ✅ Structured logging with trace ID, Actuator health/metrics/info
- ✅ System alerts (manual) and diagnostic reports (DB_HEALTH, TABLE_STATS, CONNECTION_POOL, FULL)

**Missing or incomplete:**
- ❌ Pinyin/initial letter matching in search
- ❌ DND (Do Not Disturb) window management API
- ❌ Scheduled notification queue processor
- ❌ Arrival reminder trigger (default 10 min — code uses 15 min default)
- ❌ Missed check-in notification (5 min after start)
- ❌ Workflow conditional branching / parallel approvals
- ❌ Workflow timeout escalation (24h unprocessed → warning)
- ❌ HTML template parsing for data ingestion
- ❌ Cleaning rule application during data import
- ❌ Automated P95 response time monitoring with alert generation
- ❌ Automated queue backlog monitoring with alert generation

#### 4.2.2 End-to-end deliverable

**Conclusion: Pass**

- The project includes a complete structure: Angular frontend, Spring Boot backend, PostgreSQL, Docker Compose, Flyway migrations, integration tests, scripts, documentation.
- No mock/hardcoded behavior substitutes for real logic in the implemented features. Services use real PostgreSQL queries and JPA persistence.
- The `README.md` and project structure are those of a real application, not a teaching sample.
- Evidence: 140 backend source files, 26 frontend source files, 9 migration files, 7 integration test classes.

---

### 4.3 Engineering and Architecture Quality

#### 4.3.1 Structure and module decomposition

**Conclusion: Pass**

- The backend follows a clean modular-monolith pattern with vertical feature slices: `auth/`, `transit/`, `search/`, `passenger/`, `messaging/`, `workflow/`, `admin/`, `observability/`, `ingestion/`, `config/`, `web/`.  
  Evidence: package layout in `repo/backend/src/main/java/com/eegalepoint/citybus/`
- Each module contains its own controller, service, DTOs, and references to domain entities/repositories.
- The `domain/` package is organized by sub-domain: `config/`, `messaging/`, `operations/`, `passenger/`, `search/`, `transit/`, `workflow/`.
- Frontend uses Angular standalone components with lazy-loaded routes per role.  
  Evidence: `repo/frontend/src/app/app.routes.ts:1-31`
- No redundant or unnecessary files observed. Each file serves a clear purpose.
- No excessive single-file implementations — logic is distributed across appropriate modules.

#### 4.3.2 Maintainability and extensibility

**Conclusion: Pass**

- Separation of concerns is clean: controllers handle HTTP mapping and `@PreAuthorize`, services handle business logic with `@Transactional`, repositories handle data access.
- DTOs are decoupled from entities (records for requests/responses).
- Configuration is externalized via environment variables and `application.yml`.
- Flyway migrations allow incremental schema evolution.
- The ranking config is stored in the database and dynamically loaded, allowing runtime tuning without code changes.
- The `ImportJobLifecycleService` uses `REQUIRES_NEW` propagation to ensure job status is persisted independently of import transaction outcome — a deliberate design for resilience.  
  Evidence: `ImportJobLifecycleService.java:26`

---

### 4.4 Engineering Details and Professionalism

#### 4.4.1 Error handling, logging, validation, API design

**Conclusion: Pass**

- **Error handling:** `ApiExceptionHandler` (`@RestControllerAdvice`) maps `BadCredentialsException` → 401 and `MethodArgumentNotValidException` → 400 with structured JSON responses. `JsonAuthHandlers` provides custom 401/403 JSON responses for Spring Security.  
  Evidence: `web/ApiExceptionHandler.java:13-35`, `config/JsonAuthHandlers.java:16-45`
- **Logging:** Structured log pattern with ISO timestamp, level, thread, traceId, logger, and message. Trace ID propagated via MDC.  
  Evidence: `application.yml:41-42` (`logging.pattern.console`)
- **Validation:** `@Valid` + `@NotBlank` + `@Size` on login request. Search query validated with regex (`[a-zA-Z0-9\\s\\-]{2,128}`). Resource not-found returns 404 via `ResponseStatusException`.  
  Evidence: `auth/LoginRequest.java:6-8`, `search/SearchService.java:28`
- **API design:** RESTful with consistent `/api/v1/` prefix, JSON content type, proper HTTP methods and status codes (201 for creates, 204 for deletes, 409 for conflicts).

#### 4.4.2 Real product quality

**Conclusion: Pass**

- The deliverable has all characteristics of a real application: Docker deployment, health checks, graceful shutdown, connection pool tuning, backup/restore scripts, CI workflow, structured documentation.
- ADR (Architecture Decision Record) exists explaining the monolith + DB queue decision.  
  Evidence: `repo/docs/adr/001-monolith-and-db-queue.md`
- Hikari pool tuning with leak detection at 30s, idle timeout 5min, max lifetime 20min.  
  Evidence: `application.yml:12-17`

---

### 4.5 Prompt Understanding and Requirement Fit

#### 4.5.1 Business goal alignment

**Conclusion: Partial Pass**

The core business objective — a transit coordination platform with passenger self-service, dispatcher workflows, and admin configuration — is correctly understood and implemented. The three-role model, JWT auth, PostgreSQL persistence, and offline LAN deployment constraints are all addressed.

However, several domain-specific requirements are misunderstood or omitted:

1. **Pinyin matching** is a core search feature for a Chinese transit system. The `SearchService` uses only SQL `ILIKE` with a Latin-character-only regex, providing no phonetic matching capability.  
   Evidence: `search/SearchService.java:28` (QUERY_SAFE regex), `search/SearchService.java:40-60` (SQL queries)

2. **"Frequency priority" in sorting** likely refers to route service frequency (departure count), which is not factored into the ranking algorithm. The ranking uses text-match score and stop popularity only.  
   Evidence: `search/SearchService.java:109-135` (rankCombined method)

3. **DND windows** are mentioned as a core notification preference feature. The data model exists but the API surface is missing.  
   Evidence: `V6__passenger_messaging.sql:35-42` (table), no endpoint in `PassengerController.java`

4. **Notification triggers via scheduled tasks** is an explicit architectural constraint. The message queue is write-only with no consumer.  
   Evidence: `messaging/MessageService.java:76-79` (creates queue entry), no `@Scheduled` class in entire codebase

5. **Default reminder of 10 minutes** — code defaults to 15 minutes.  
   Evidence: `passenger/PassengerService.java:141` (returns 15), `V6__passenger_messaging.sql:30` (DB default 15)

---

### 4.6 Aesthetics (Full-stack)

#### 4.6.1 Visual and interaction design

**Conclusion: Partial Pass**

**Positives:**
- Tab-based navigation clearly separates functional areas in each role page (Passenger: 5 tabs, Dispatcher: 2 tabs, Admin: 8 tabs).
- Consistent visual language: `system-ui, sans-serif` font stack, consistent spacing, table styling with alternating header backgrounds.
- Status badges use color-coded backgrounds (`data-status` attribute) for visual differentiation (green for confirmed/approved, red for cancelled/rejected, yellow for pending).  
  Evidence: `passenger.component.scss:81-89`
- Interactive feedback: disabled buttons during loading, `.active` tab indicator with blue border-bottom.
- Message list distinguishes unread messages with a blue left border.  
  Evidence: `passenger.component.scss:127` (`&.unread { border-left: 3px solid #1a73e8; }`)

**Gaps:**
- **Minimal styling depth:** The UI is functional but austere. No color scheme beyond gray/blue, no icons, no illustrations, no branded header or logo beyond text.
- **Global styles are empty:** `styles.scss` contains only a CSS comment — no base reset, no CSS variables, no theme tokens.  
  Evidence: `frontend/src/styles.scss:1`
- **No hover states on table rows or action buttons** (except default browser styles).
- **No transitions or animations** for tab switching, loading states, or feedback.
- **No responsive considerations** beyond `max-width` constraints on `.page` containers. No media queries, no mobile breakpoints.
- **Duplicate SCSS** across all three feature components — identical `.page`, `.page__nav`, `.tabs`, `table`, `.badge`, `.btn-sm`, `.btn-danger` styles are copy-pasted rather than shared via global styles or mixins.

---

## 5. Issues / Suggestions (Severity-Rated)

### Issue 1 — Pinyin/Initial Letter Search Matching Not Implemented

**Severity:** High  
**Conclusion:** Missing  
**Evidence:** `search/SearchService.java:28` (regex `[a-zA-Z0-9\\s\\-]{2,128}`), `search/SearchService.java:40-60` (SQL uses ILIKE only)  
**Impact:** A core Prompt requirement for Chinese transit search — passengers cannot search stops/routes by pinyin romanization or initial letters. The search only matches exact substrings.  
**Minimum actionable fix:** Implement a pinyin conversion layer (e.g., `pinyin4j` or a lookup table) that maps Chinese stop/route names to their pinyin representations, and extend the SQL queries to match against both original names and pinyin equivalents.

### Issue 2 — Do-Not-Disturb Window API Endpoints Missing

**Severity:** High  
**Conclusion:** Table exists, API missing  
**Evidence:** `V6__passenger_messaging.sql:35-42` (table `do_not_disturb_windows`), `domain/passenger/DoNotDisturbWindowEntity.java` (entity exists), `passenger/PassengerController.java` (no DND endpoints)  
**Impact:** Passengers cannot set DND periods (e.g., 22:00–07:00) as required by the Prompt. The data model is ready but the feature is not exposed.  
**Minimum actionable fix:** Add `GET/POST/DELETE /api/v1/passenger/dnd-windows` endpoints to `PassengerController` with corresponding service methods.

### Issue 3 — No Scheduled Message Queue Consumer

**Severity:** High  
**Conclusion:** Missing  
**Evidence:** `domain/messaging/MessageQueueEntity.java`, `domain/messaging/MessageQueueAttemptEntity.java` (entities exist), no `@Scheduled` class in any source file  
**Impact:** The Prompt explicitly requires "notifications are uniformly written to an in-platform message queue and triggered by local scheduled tasks." Messages are queued (`message_queue`) but never processed — the queue is write-only. No delivery attempts are ever created in `message_queue_attempts`.  
**Minimum actionable fix:** Create a `@Scheduled` service that polls `message_queue` for `QUEUED` entries, processes them (marks as `SENT`), records attempts in `message_queue_attempts`, and handles retries for failures.

### Issue 4 — Workflow Timeout Escalation Not Implemented

**Severity:** High  
**Conclusion:** Table exists, scheduler missing  
**Evidence:** `V7__workflow.sql:51-58` (table `workflow_escalations`), `domain/workflow/WorkflowEscalationEntity.java` (entity exists), no timeout monitoring logic in any service  
**Impact:** The Prompt requires "tasks unprocessed for 24 hours trigger escalation warnings." No scheduler monitors task age or creates escalation entries.  
**Minimum actionable fix:** Create a `@Scheduled` job that queries `workflow_tasks` for `PENDING` tasks older than 24 hours and inserts `workflow_escalations` records plus system alerts.

### Issue 5 — Cleaning Rules Not Applied During Data Import

**Severity:** High  
**Conclusion:** Rules configurable but never invoked  
**Evidence:** `admin/AdminConfigController.java` (CRUD for cleaning rules), `ingestion/CanonicalImportTransactionalService.java:56-94` (import logic — no call to cleaning service), `domain/config/CleaningRuleSetEntity.java` (entity exists)  
**Impact:** The Prompt requires stop names and other fields to undergo "standardized cleaning and missing value handling" with configurable rules and audit logs. Rules can be created via admin API but are never executed against incoming data. The `cleaning_audit_logs` table will always remain empty.  
**Minimum actionable fix:** Inject `CleaningRuleSetRepository` into `CanonicalImportTransactionalService`, apply enabled rules to relevant fields (stop name, etc.) during import, and record transformations in `cleaning_audit_logs`.

### Issue 6 — Hardcoded Default JWT Secret

**Severity:** High  
**Conclusion:** Security risk  
**Evidence:** `application.yml:37` (`secret: ${APP_JWT_SECRET:0123456789abcdef...}`), `.env.example:12` (same value)  
**Impact:** If the `APP_JWT_SECRET` environment variable is not explicitly set, the application uses a well-known default secret. Any attacker who knows this (it's in the repository) can forge valid JWTs. The `.env.example` also contains this value, encouraging copy-paste deployment with the insecure default.  
**Minimum actionable fix:** Remove the default value from `application.yml` so the application fails to start without an explicitly configured secret. Update `.env.example` to use a placeholder like `CHANGE_ME_TO_RANDOM_VALUE`.

### Issue 7 — Reminder Default Mismatches Prompt Specification

**Severity:** Medium  
**Conclusion:** Implementation deviates from Prompt  
**Evidence:** `passenger/PassengerService.java:141` (default `minutesBefore: 15`), `V6__passenger_messaging.sql:30` (`DEFAULT 15`). Prompt specifies "default 10 minutes in advance."  
**Impact:** Minor functional deviation — reminders default to 15 minutes instead of the specified 10.  
**Minimum actionable fix:** Change default to 10 in `V6__passenger_messaging.sql` and `PassengerService.getReminderPreferences()`.

### Issue 8 — Workflow Conditional Branching / Parallel Approvals Not Implemented

**Severity:** Medium  
**Conclusion:** Partial implementation  
**Evidence:** `workflow/WorkflowService.java` — workflow is a flat task list with approve/reject/return. No conditional logic, no branching paths, no parallel approval gates.  
**Impact:** The Prompt requires "conditional branching, joint/parallel approvals." The current engine supports only linear task sequences with individual decisions.  
**Minimum actionable fix:** Add branching logic to `WorkflowDefinitionEntity` (e.g., JSON-based step definitions with conditions) and implement parallel approval gates (require N of M approvals).

### Issue 9 — HTML Template Parsing Not Supported

**Severity:** Medium  
**Conclusion:** Missing  
**Evidence:** `ingestion/CanonicalImportService.java` — only accepts `CanonicalImportRequest` (JSON). No HTML parsing logic anywhere in the codebase.  
**Impact:** The Prompt states "bus data integration supports structured parsing of HTML/JSON templates." Only JSON is implemented.  
**Minimum actionable fix:** Add an HTML parser (e.g., Jsoup) and a new source type/template for HTML-based data ingestion.

### Issue 10 — No Automated P95/Queue Backlog Alerting

**Severity:** Medium  
**Conclusion:** Missing  
**Evidence:** `admin/OperationsService.java` — alerts are manually created only. No automated monitoring of API response times or queue depths.  
**Impact:** The Prompt requires "Queue backlogs and API P95 response times exceeding 500ms trigger local alerts and diagnostic reports."  
**Minimum actionable fix:** Create a `@Scheduled` job that queries Actuator metrics for P95 response times and `message_queue` for backlog depth, creating system alerts when thresholds are exceeded.

### Issue 11 — Missed Check-in Notification Logic Not Implemented

**Severity:** Medium  
**Conclusion:** Missing  
**Evidence:** No logic in `PassengerService.java` or any scheduler to detect missed check-ins (5 minutes after start time) and send notifications.  
**Impact:** A specific Prompt requirement for the notification system.  
**Minimum actionable fix:** Create a scheduled job comparing reservation departure times against check-in records and generating notifications for missed check-ins.

### Issue 12 — Search Query Validation Blocks Non-Latin Characters

**Severity:** Medium  
**Conclusion:** Over-restrictive validation  
**Evidence:** `search/SearchService.java:28` — `Pattern.compile("^[a-zA-Z0-9\\s\\-]{2,128}$")`  
**Impact:** While the Prompt specifies an "English interface," the underlying transit data may contain non-ASCII characters (especially if stop names are in Chinese). The regex blocks any search containing non-Latin characters, which could prevent legitimate searches.  
**Minimum actionable fix:** Expand the regex to allow Unicode letters (`\\p{L}`) or at minimum CJK character ranges, depending on the expected data.

### Issue 13 — Duplicate SCSS Across Feature Components

**Severity:** Low  
**Conclusion:** Maintainability concern  
**Evidence:** `passenger.component.scss`, `dispatcher.component.scss`, `admin.component.scss` — identical `.page`, `.tabs`, `table`, `.badge`, `.btn-sm` rulesets duplicated across all three.  
**Impact:** Style changes require updating three files. No shared utility classes or mixins.  
**Minimum actionable fix:** Extract shared styles into `styles.scss` or a shared SCSS partial.

### Issue 14 — Empty Global Stylesheet

**Severity:** Low  
**Conclusion:** Minor quality gap  
**Evidence:** `frontend/src/styles.scss:1` — contains only a CSS comment, no base styles.  
**Impact:** No CSS reset/normalize, no CSS custom properties for theming, no shared component styles.  
**Minimum actionable fix:** Add a base reset and shared design tokens (colors, spacing, typography).

### Issue 15 — CORS Wide Open

**Severity:** Low (context-dependent)  
**Conclusion:** Acceptable for offline LAN per Prompt, but noted  
**Evidence:** `config/SecurityBeansConfig.java:16` — `c.setAllowedOriginPatterns(List.of("*"))`  
**Impact:** For the specified offline LAN deployment, this is acceptable. For any internet-facing deployment, this would be a security concern.  
**Minimum actionable fix:** Make CORS origins configurable via environment variable for production hardening.

---

## 6. Security Review Summary

### Authentication entry points

**Conclusion: Pass**

- `POST /api/v1/auth/login` is the sole authentication entry point, accepting `{ username, password }` with `@Valid` and `@Size(min=8)` on password.  
  Evidence: `auth/AuthController.java:23-26`, `auth/LoginRequest.java:6-8`
- `AuthService.login()` loads user by username, verifies `enabled` flag, checks BCrypt hash, creates JWT, and records login audit.  
  Evidence: `auth/AuthService.java:42-62`
- Failed login attempts (invalid user, disabled user, wrong password) all return 401 with identical error messages, preventing username enumeration.  
  Evidence: `auth/AuthService.java:45-55`

### Route-level authorization

**Conclusion: Pass**

- `SecurityConfig` enforces authentication on all `/api/v1/**` routes except `POST /api/v1/auth/login`.  
  Evidence: `config/SecurityConfig.java:35-44`
- Actuator endpoints are public (consistent with Prompt's health check requirement).
- Method-level RBAC via `@PreAuthorize`:
  - `hasRole('ADMIN')`: `AdminConfigController`, `AdminImportController`, `OperationsController`
  - `hasAnyRole('ADMIN','DISPATCHER')`: `WorkflowController`, `TaskController`
  - `hasAnyRole('ADMIN','PASSENGER')`: `PassengerController`
  - `isAuthenticated()`: `RouteController`, `StopController`, `SearchController`, `MessageController`
- Evidence verified in every controller file.

### Object-level authorization

**Conclusion: Pass**

- `MessageService.getMessage()` and `markRead()` use `findByIdAndUser_Id(id, user.getId())` — ensures users can only access their own messages.  
  Evidence: `messaging/MessageService.java:54-57`, `messaging/MessageService.java:62-67`
- `PassengerService.updateReservation()` checks `!reservation.getUser().getId().equals(user.getId())` and returns 403.  
  Evidence: `passenger/PassengerService.java:92-94`
- `PassengerService.createCheckin()` checks reservation ownership when `reservationId` is provided.  
  Evidence: `passenger/PassengerService.java:108-112`

### Function-level authorization

**Conclusion: Pass**

- `@EnableMethodSecurity` is enabled on `SecurityConfig`.  
  Evidence: `config/SecurityConfig.java:18`
- Every controller method has an appropriate `@PreAuthorize` annotation. Class-level `@PreAuthorize("hasRole('ADMIN')")` on `AdminConfigController` and `OperationsController` provides blanket admin-only enforcement.

### Tenant / user data isolation

**Conclusion: Pass**

- All passenger endpoints scope queries by `currentUser()` — derived from the JWT's `SecurityContext`.
- Messages, reservations, check-ins, and preferences are all filtered by user ID.
- No cross-user data leakage paths identified in the reviewed service code.

### Admin / internal / debug endpoint protection

**Conclusion: Partial Pass**

- Admin endpoints are protected by `@PreAuthorize("hasRole('ADMIN')")`.  
- `RoleDemoController` at `/api/v1/demo/admin|dispatcher|passenger` exists and is protected by appropriate `@PreAuthorize`.  
  Evidence: `rbac/RoleDemoController.java:13-30`
- **Concern:** The `/api/v1/demo/*` endpoints are demo/debug endpoints that should be removed in production. They don't expose sensitive data but are unnecessary.
- **Concern:** Actuator endpoints including `/actuator/env` are publicly accessible, which could expose environment variables (including database credentials) to unauthenticated users.  
  Evidence: `config/SecurityConfig.java:37` (`/actuator/**` is `permitAll()`), `application.yml:30` (env endpoint exposed)

---

## 7. Tests and Logging Review

### Unit tests

**Conclusion: Fail**

- **Backend:** No standalone unit tests exist. All 7 test classes are integration tests (`@SpringBootTest` with `@Testcontainers`).
- **Frontend:** Only one test file `app.component.spec.ts` with 3 trivial smoke tests (component creation, title check, router-outlet render). No tests for `AuthService`, guards, interceptors, or feature components.  
  Evidence: `frontend/src/app/app.component.spec.ts:1-23`

### API / integration tests

**Conclusion: Partial Pass**

- 7 integration test classes using Testcontainers with real PostgreSQL:
  - `CityBusApplicationIT` (9 tests): health, seed data, login/auth, RBAC enforcement
  - `TransitCanonicalImportIT` (2 tests): admin import + passenger denial
  - `SearchApiIT` (5 tests): auth requirement, query validation, suggestions, results with impressions, stop detail
  - `PassengerMessagingIT` (10 tests): reservation CRUD, check-in, reminders, messages, queue, dispatcher denial
  - `WorkflowIT` (8 tests): auth, RBAC, create workflow/tasks, approve/reject/return flow, batch, double-decision 409, status filtering
  - `AdminConsoleIT` (7 tests): auth, RBAC, cleaning rule CRUD, dictionary CRUD, ranking config, templates, users, audit
  - `ObservabilityIT` (9 tests): health, metrics, trace header, alert CRUD with acknowledge, double-ack 409, diagnostics (DB_HEALTH, TABLE_STATS, FULL), diagnostics list
- Test framework: JUnit 5 + Spring Boot Test + Testcontainers + AssertJ
- Test entry point: `repo/run_test.sh` or `scripts/mvn-verify-docker.sh`
- Evidence: `repo/backend/src/test/java/com/eegalepoint/citybus/*.java`

**Gaps:**
- No cross-user object-level authorization tests (e.g., user A reading user B's messages)
- No tests for DND windows (feature not exposed)
- No tests for message queue consumption (no consumer exists)
- No tests for workflow escalation timeouts (no scheduler exists)

### Logging categories / observability

**Conclusion: Pass**

- Structured log pattern with timestamp, level, thread, traceId, logger name:  
  `%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%thread] [%X{traceId:-}] %logger{36} - %msg%n`  
  Evidence: `application.yml:42`
- `TraceIdFilter` propagates `X-Trace-Id` header and sets MDC for all log lines.  
  Evidence: `observability/TraceIdFilter.java:1-37`
- Login audit records attempts with IP, user-agent, success/failure, and user ID.  
  Evidence: `audit/LoginAuditService.java:18-34`
- Actuator exposes `health`, `info`, `metrics`, `flyway`, `env`.  
  Evidence: `application.yml:28-29`

### Sensitive-data leakage risk in logs / responses

**Conclusion: Partial Pass**

- Login error messages are generic ("Invalid credentials") — no username enumeration.  
  Evidence: `auth/AuthService.java:46, 50, 54`
- Password hashes are never exposed in any response DTO. `UserAdminResponse` returns only `id, username, enabled, roles, createdAt`.
- `LoginAuditService` stores `user_agent` (truncated to 512 chars) — this is appropriate for audit.
- **Concern:** `/actuator/env` is publicly accessible and could expose `APP_JWT_SECRET`, database credentials, and other environment variables.  
  Evidence: `config/SecurityConfig.java:37` (`/actuator/**` permitAll), `application.yml:29` (`env` in exposed endpoints)
- **Concern:** Diagnostic reports (`OperationsService.runDbHealth()`) return the full PostgreSQL version string, which could aid attackers.  
  Evidence: `admin/OperationsService.java:106`

---

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview

- **Unit tests:** Not present for backend services. 1 trivial frontend smoke test.
- **API / integration tests:** 7 test classes, ~50 test methods, using JUnit 5 + Spring Boot Test + Testcontainers + AssertJ.
- **Test framework:** JUnit Jupiter 5 (via `spring-boot-starter-test`), Testcontainers 1.20.4 (via BOM).
- **Test entry points:** `repo/run_test.sh:6` (Maven verify via Docker), `repo/scripts/mvn-verify-docker.sh`
- **Documentation:** `repo/README.md:81-101` provides test commands for both Docker-based and host-based execution.
- **Test config:** `application-test.yml` activates `test` profile with `ddl-auto: validate`.  
  Evidence: `backend/src/test/resources/application-test.yml:1-8`

### 8.2 Coverage Mapping Table

| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion / Fixture | Coverage | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| Login happy path | `CityBusApplicationIT.loginSuccessAndPingWithBearer()` | Asserts 200, `accessToken` present, ping returns OK | **Sufficient** | — | — |
| Login bad password → 401 | `CityBusApplicationIT.loginBadPasswordReturns401()` | Asserts 401 | **Sufficient** | — | — |
| Login short password → 400 | `CityBusApplicationIT.loginShortPasswordReturns400()` | Asserts 400 | **Sufficient** | — | — |
| Unauthenticated → 401 | `CityBusApplicationIT.apiPingRequiresAuthentication()`, `SearchApiIT.suggestionsAndResultsRequireAuth()`, `PassengerMessagingIT.reservationsRequireAuth()`, `WorkflowIT.workflowsRequireAuth()`, `AdminConsoleIT.adminEndpointsRequireAuth()` | Assert 401 across 5 test classes | **Sufficient** | — | — |
| RBAC passenger ✗ admin | `CityBusApplicationIT.rbacPassengerCannotAccessAdminDemo()` | Asserts 403 | **Basically covered** | No test for passenger accessing workflow endpoints | Add test |
| RBAC dispatcher ✗ admin | `AdminConsoleIT.nonAdminCannotAccessAdminEndpoints()` | Asserts 403 on cleaning-rules and users | **Basically covered** | — | — |
| RBAC dispatcher ✗ passenger | `PassengerMessagingIT.dispatcherCannotAccessPassengerReservations()` | Asserts 403 | **Basically covered** | — | — |
| RBAC passenger ✗ import | `TransitCanonicalImportIT.passengerCannotRunImport()` | Asserts 403 | **Sufficient** | — | — |
| Canonical JSON import | `TransitCanonicalImportIT.adminCanImportCanonicalJsonAndQueryRoutes()` | Asserts SUCCEEDED status, route in DB, job recorded | **Sufficient** | — | — |
| Search suggestions & results | `SearchApiIT.suggestionsReturnRoutesAndStops()`, `SearchApiIT.resultsIncrementImpressionsAndLogSearchEvent()` | Assert non-empty results, impression count, search_events | **Sufficient** | — | — |
| Search query validation | `SearchApiIT.shortQueryReturns400()` | Asserts 400 for single-char query | **Basically covered** | No test for invalid characters | Add test |
| Reservation create + list | `PassengerMessagingIT.passengerCanCreateAndListReservation()` | Assert 201, PENDING status, list size 1 | **Sufficient** | — | — |
| Reservation cancel | `PassengerMessagingIT.passengerCanCancelReservation()` | Assert CANCELLED status | **Sufficient** | — | — |
| Check-in | `PassengerMessagingIT.passengerCanCheckin()` | Assert 201, stopCode | **Sufficient** | — | — |
| Reminder preferences | `PassengerMessagingIT.reminderPreferencesDefaultValues()`, `.passengerCanUpdateReminderPreferences()` | Assert defaults + update persistence | **Sufficient** | — | — |
| Message create + read + mark | `PassengerMessagingIT.messageMarkReadFlow()` | Assert 201, read=false→true flow | **Sufficient** | — | — |
| Reservation → notification | `PassengerMessagingIT.reservationCreatesMessage()` | Assert message exists + queue count ≥ 1 | **Sufficient** | — | — |
| Workflow create + tasks | `WorkflowIT.dispatcherCanCreateWorkflowAndTasks()` | Assert 201, OPEN status, task PENDING | **Sufficient** | — | — |
| Approve/reject/return flow | `WorkflowIT.approveRejectReturnFlow()` | Assert status transitions, instance COMPLETED | **Sufficient** | — | — |
| Reject → instance REJECTED | `WorkflowIT.rejectTaskSetsWorkflowRejected()` | Assert instance REJECTED | **Sufficient** | — | — |
| Double decision → 409 | `WorkflowIT.doubleDecisionReturnsConflict()` | Assert 409 | **Sufficient** | — | — |
| Batch task decisions | `WorkflowIT.batchApprove()` | Assert processed count = 2 | **Sufficient** | — | — |
| Admin cleaning rule CRUD | `AdminConsoleIT.cleaningRuleCrud()` | Assert 201, list, update, 204 delete | **Sufficient** | — | — |
| Admin dictionary CRUD | `AdminConsoleIT.dictionaryCrud()` | Assert 201, list, update, 204 delete | **Sufficient** | — | — |
| Admin ranking config | `AdminConsoleIT.rankingConfigGetAndUpdate()` | Assert get + update values persist | **Sufficient** | — | — |
| Admin user enable/disable | `AdminConsoleIT.usersListAndUpdate()` | Assert disable + re-enable | **Sufficient** | — | — |
| System alerts CRUD + ack | `ObservabilityIT.alertCreateListAcknowledge()` | Assert 201, list, ack=true, unack filter | **Sufficient** | — | — |
| Double ack → 409 | `ObservabilityIT.doubleAcknowledgeReturnsConflict()` | Assert 409 | **Sufficient** | — | — |
| Diagnostic reports | `ObservabilityIT.diagnosticDbHealth()`, `.diagnosticTableStats()`, `.diagnosticFull()` | Assert COMPLETED, summary content | **Sufficient** | — | — |
| Trace ID header | `CityBusApplicationIT.loginSuccessAndPingWithBearer()`, `ObservabilityIT.traceIdHeaderReturned()` | Assert `X-Trace-Id` not blank | **Sufficient** | — | — |
| DND window management | — | — | **Missing** | No API exists | Implement feature + tests |
| Notification queue processing | — | — | **Missing** | No scheduler exists | Implement scheduler + tests |
| Workflow timeout escalation | — | — | **Missing** | No scheduler exists | Implement scheduler + tests |
| Cleaning rule application | — | — | **Missing** | Rules not called during import | Integrate + test |
| Cross-user message isolation | — | — | **Missing** | No test verifies user A cannot read user B's messages | Add cross-user 403 test |
| Cross-user reservation isolation | — | — | **Missing** | No test verifies user A cannot cancel user B's reservations | Add cross-user 403 test |
| Pinyin search matching | — | — | **Missing** | Feature not implemented | Implement + test |

### 8.3 Security Coverage Audit

| Security dimension | Test coverage | Assessment |
|---|---|---|
| **Authentication** | Login success/failure/short-password tested across multiple IT classes. 401 for unauthenticated requests tested in 5 classes. | **Sufficient** — core auth flow is well-tested |
| **Route authorization** | Dispatcher ✗ admin, passenger ✗ admin, passenger ✗ import, passenger ✗ workflow all tested | **Basically covered** — most RBAC boundaries tested |
| **Object-level authorization** | Reservation ownership checked in code (`PassengerService:92`), message isolation via `findByIdAndUser_Id`. | **Insufficient** — no cross-user tests exist; logic is correct but untested |
| **Tenant / data isolation** | All queries scope by `currentUser()` | **Insufficient** — no test verifies that user A's data is invisible to user B |
| **Admin / internal protection** | Admin endpoints tested for 401 (unauth) and 403 (dispatcher) | **Basically covered** — but `/actuator/env` exposure is untested |

### 8.4 Final Coverage Judgment

**Conclusion: Partial Pass**

**Covered risks:**
- Authentication flow (login, token validation, 401/403)
- Route-level RBAC (most role-endpoint combinations tested)
- Core CRUD flows (reservations, check-ins, messages, workflows, tasks, admin config, alerts, diagnostics)
- Double-decision / double-acknowledge conflict detection (409)
- Search validation, ranking, and impression tracking
- Data import with job lifecycle tracking

**Uncovered risks that mean tests could still pass while severe defects remain:**
- Cross-user data isolation is enforced in code but never tested — a regression removing `findByIdAndUser_Id` would pass all existing tests while allowing any user to read any user's messages
- No tests for DND, notification scheduling, escalation timeouts, or cleaning rule application — because these features are not implemented
- No frontend tests beyond a trivial smoke test — UI regressions would be undetected
- `/actuator/env` public exposure is not tested or validated

---

## 9. Final Notes

- The project demonstrates strong engineering fundamentals: clean architecture, proper separation of concerns, comprehensive integration tests with real PostgreSQL, professional deployment tooling, and thorough documentation.
- The primary deficits are in **feature completeness** rather than **engineering quality**. Several Prompt requirements are structurally prepared (tables, entities) but lack the processing logic (schedulers, API endpoints, business rules) to function.
- The hardcoded JWT secret default is the most acute security concern and should be addressed before any deployment.
- The Actuator `/env` endpoint being publicly accessible could leak sensitive configuration and should be restricted to authenticated users or removed from the exposed endpoint list.
- All conclusions are based on static code analysis. Runtime behavior, Docker Compose startup, and test execution results require manual verification.
