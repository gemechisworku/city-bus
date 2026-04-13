# City Bus Platform — Delivery Acceptance & Architecture Audit

**Date:** 2026-04-13  
**Auditor:** Static-only review (no runtime, no Docker, no test execution)  
**Repository:** `d:\EegalePointAI\city-bus\repo`

---

## 1. Verdict

**Overall conclusion: Partial Pass**

The project demonstrates a well-structured, end-to-end full-stack delivery covering the three user roles (passenger, dispatcher, admin) with a Spring Boot backend, Angular frontend, PostgreSQL database, and Docker Compose orchestration. Core architectural decisions are sound (JWT auth, Flyway migrations, Testcontainers integration tests, RBAC enforcement via `@PreAuthorize`). The backend has 47 integration test methods across 7 test classes covering major flows.

However, several prompt-mandated features are **missing or only partially implemented**, and there is a **High-severity security configuration issue**. These prevent a full Pass.

---

## 2. Scope and Static Verification Boundary

### What was reviewed
- All 138 Java source files under `backend/src/main/java/`
- All 7 integration test classes + test configuration
- All 9 Flyway SQL migration scripts
- All Angular source files (app root, auth module, 3 feature components + specs)
- Docker Compose, Dockerfiles, build files (`pom.xml`, `angular.json`, `package.json`)
- All documentation (`README.md`, `docs/api-outline.md`, `docs/data-model-erd.md`, `docs/implementation-status.md`, `docs/acceptance-checklist.md`)
- All scripts (`run_test.sh`, `mvn-verify-docker.sh`, backup/restore scripts)
- Configuration files (`application.yml`, `application-test.yml`, `.env.example`, `proxy.conf.json`)

### What was not reviewed
- `.angular/cache/` build artifacts
- `backups/` directory content
- CI workflow file (`.github/workflows/ci.yml`) — not in scope

### What was intentionally not executed
- Docker Compose startup
- Maven build / test execution
- Angular build / Karma tests
- Any database operations

### Claims requiring manual verification
- Docker Compose builds and starts successfully
- All 47 integration tests pass
- Angular production build completes without errors
- Frontend renders correctly in browser
- Backup/restore scripts function correctly

---

## 3. Repository / Requirement Mapping Summary

### Prompt core business goal
A "City Bus Operation and Service Coordination Platform" with three roles: passengers (search, reservations, reminders, messages), dispatchers (workflow/task dashboard), and administrators (config, templates, rules, dictionaries).

### Core flows from prompt
1. Passenger search with autocomplete, pinyin/initial matching, frequency+popularity ranking, deduplication
2. Notification preferences with DND windows (22:00–07:00 example)
3. Message center with reservation/reminder/missed-checkin notifications
4. Dispatcher workflow with conditional branching, joint/parallel approvals, returns, timeout escalation
5. Admin management of notification templates, ranking weights, field dictionaries, cleaning rules with audit logs
6. Data ingestion with HTML/JSON parsing, field mapping, version management, standardized cleaning
7. Local auth with password ≥8 chars, salted hashing
8. In-platform message queue with scheduled task triggering
9. Observability with trace IDs, structured logging, health checks, P95 alerts

### Main implementation areas mapped
| Prompt requirement | Implementation area | Status |
|---|---|---|
| Three user roles | Auth + RBAC (`SecurityConfig`, `@PreAuthorize`, Angular guards) | Implemented |
| Passenger search | `SearchController`/`SearchService`, ranking config | Partially implemented (no pinyin) |
| Reservations/check-ins | `PassengerController`/`PassengerService` | Implemented |
| DND windows | `do_not_disturb_windows` table only — no API | Missing API |
| Message center | `MessageController`/`MessageService` | Implemented |
| Notification processing | `message_queue` table — no consumer | Missing processor |
| Workflow engine | `WorkflowController`/`TaskController`/`WorkflowService` | Partially (no timeout escalation) |
| Admin config | `AdminConfigController`/`AdminConfigService` | Implemented |
| Data ingestion | `CanonicalImportService` — JSON only | Partial (no HTML, no cleaning integration) |
| Observability | `TraceIdFilter`, Actuator, diagnostics | Implemented |

---

## 4. Section-by-section Review

### Section 1: Hard Gates

#### 1.1 Documentation and Static Verifiability
**Conclusion: Pass**

| Aspect | Evidence | Assessment |
|---|---|---|
| Startup instructions | `README.md:56-60` — `docker compose up --build` | Clear |
| Test instructions | `README.md:83-103` — `run_test.sh`, `mvn-verify-docker.sh/.ps1` | Clear |
| Configuration | `README.md:43-50` — `.env.example`, JWT secret, ports | Clear |
| Seed credentials | `README.md:62-70` — admin/dispatcher1/passenger1 with passwords | Clear |
| Project structure | `README.md:16-28` — layout table | Clear |
| API documentation | `docs/api-outline.md` — comprehensive endpoint listing | Clear |
| Data model | `docs/data-model-erd.md` — entity groups with mermaid diagram | Clear |

**Rationale:** Documentation is comprehensive, internally consistent with the codebase, and provides sufficient information for a human reviewer to attempt verification without rewriting code.

#### 1.2 Prompt Deviation
**Conclusion: Partial Pass**

**Rationale:** The implementation is centered on the business goal described in the prompt. The three user roles, core CRUD operations, workflow engine, search, and admin console all align with the prompt. However, several prompt-explicit features are missing (detailed in Section 5), constituting a material deviation from the stated requirements.

**Evidence of alignment:**
- Three-role RBAC: `SecurityConfig.java:35-44`, `V2__seed_roles_and_admin.sql`
- Search with ranking: `SearchService.java:74-103`
- Workflow: `WorkflowService.java:53-171`
- Admin config: `AdminConfigController.java:28-132`

**Evidence of deviation:**
- No pinyin matching: `SearchService.java:27` — regex `[a-zA-Z0-9\\s\\-]` ASCII only
- No scheduled notification processing: grep for `@Scheduled` returns zero results
- No DND window API: no controller endpoints referencing `DoNotDisturbWindow`
- No HTML template parsing: `CanonicalImportService.java` — JSON-only import

---

### Section 2: Delivery Completeness

#### 2.1 Core Requirement Coverage
**Conclusion: Partial Pass**

| Requirement | Implemented | Evidence |
|---|---|---|
| Passenger search by route/stop/keyword | Yes | `SearchController.java:22-34` |
| Autocomplete suggestions | Partial (no pinyin) | `SearchService.java:74-85` |
| Frequency + popularity ranking | Yes | `SearchService.java:117-154` |
| Deduplication | Implicit (unique codes) | `V4__transit_canonical.sql:3` unique constraint |
| Notification preferences (toggle, DND) | Partial (toggle yes, DND table only) | `PassengerController.java:67-78`, no DND API |
| Message center | Yes | `MessageController.java:20-52` |
| Reservation → notification auto-queue | Yes | `PassengerService.java:81-86` |
| Dispatcher workflow with branching | Partial (linear approve/reject/return, no conditional branching) | `WorkflowService.java:109-156` |
| Joint/parallel approvals | No | Not modeled in workflow schema |
| Task returns | Yes | `WorkflowService.java:119-122` |
| Timeout escalation (24h) | No | No `@Scheduled` task; `workflow_escalations` table exists but unused |
| Batch processing | Yes | `TaskController.java:70-74` |
| Admin notification templates | Partial (field mapping templates only) | `AdminConfigController.java:41-44` |
| Cleaning rules with audit | Partial (CRUD exists, audit table exists, but rules not applied in import) | `AdminConfigService.java:57-100` |
| HTML/JSON template parsing | Partial (JSON only) | `CanonicalImportService.java:51-67` |
| Field standardization/cleaning in import | No | `CanonicalImportTransactionalService` does not use cleaning rules |
| Password ≥8 + salted hash | Yes | `LoginRequest.java:7-8`, `PasswordConfig.java` (BCrypt) |
| Message desensitization | Yes | `MessageService.java:74-81` |
| In-platform message queue | Partial (queue table, no consumer) | `V6__passenger_messaging.sql:62-71`, no `@Scheduled` |
| Structured logging with trace IDs | Yes | `TraceIdFilter.java:14-37`, `application.yml:56-60` |
| Health checks | Yes | `application.yml:36-48` |
| P95 alerts / diagnostic reports | Partial (manual diagnostics, no auto P95 alerts) | `OperationsService.java:88-129` |

#### 2.2 End-to-end Deliverable
**Conclusion: Pass**

**Rationale:** The project is a complete end-to-end deliverable with:
- Full project structure (backend + frontend + Docker + scripts + docs + tests)
- No mock/stub/fake behavior in production code
- Real PostgreSQL persistence via JPA/Flyway
- Proper project documentation
- Integration test suite

**Evidence:** `README.md:1-130`, `docker-compose.yml:1-55`, 138 Java source files, Angular workspace with 3 feature components

---

### Section 3: Engineering and Architecture Quality

#### 3.1 Structure and Module Decomposition
**Conclusion: Pass**

**Rationale:** The project follows a clean layered architecture:
- **Controllers** → **Services** → **Repositories/Entities** pattern consistently applied
- Domain entities organized by bounded context (`domain/passenger/`, `domain/transit/`, `domain/workflow/`, etc.)
- DTOs separated into `dto/` packages per module
- Clear separation between auth, admin, passenger, search, transit, messaging, workflow, ingestion, and observability packages

**Evidence:**
- 16 distinct packages under `com.eegalepoint.citybus`
- Each business domain has controller + service + DTO + entity layers
- No monolithic god-class pattern detected

#### 3.2 Maintainability and Extensibility
**Conclusion: Pass**

**Rationale:** The codebase shows reasonable maintainability:
- Ranking config externalized in DB (`ranking_config` table) rather than hardcoded
- Cleaning rules configurable via CRUD API
- Field mappings support template-based import
- Workflow definitions are seed-data configurable
- Spring profiles (`test`) for test configuration separation
- `open-in-view: false` (`application.yml:17`) prevents lazy-loading anti-patterns

**Evidence:** `application.yml:16-17`, `AdminConfigService.java:146-171`, `V7__workflow.sql:12-17`

---

### Section 4: Engineering Details and Professionalism

#### 4.1 Error Handling, Logging, Validation, API Design
**Conclusion: Pass**

**Rationale:**
- Global exception handler: `ApiExceptionHandler.java:14-35` catches `BadCredentialsException` → 401, `MethodArgumentNotValidException` → 400
- Service-level exceptions use `ResponseStatusException` with appropriate HTTP codes (404, 403, 409, 400)
- Input validation via Jakarta `@Valid`, `@NotBlank`, `@Size` on request DTOs
- Structured logging with MDC trace ID: `application.yml:59-60` pattern includes `[%X{traceId:-}]`
- RESTful API design with consistent path prefixes (`/api/v1/`)

**Evidence:**
- `LoginRequest.java:6-8` — `@NotBlank @Size(min=8, max=128)` on password
- `WorkflowService.java:143-144` — 409 CONFLICT on double-decision
- `PassengerService.java:108-109` — 403 on non-owner reservation access
- `TraceIdFilter.java:25-30` — trace ID propagation on every request

#### 4.2 Product-like Organization
**Conclusion: Pass**

**Rationale:** The deliverable resembles a real application:
- Docker Compose with healthchecks, dependency ordering, named volumes
- `.env.example` for configuration
- Graceful shutdown configured (`application.yml:30-33`)
- Hikari pool tuning with leak detection (`application.yml:8-14`)
- Backup/restore scripts for PostgreSQL
- CI workflow file present (`.github/workflows/ci.yml`)
- nginx reverse proxy for frontend with API proxying

---

### Section 5: Prompt Understanding and Requirement Fit

#### 5.1 Business Goal and Constraint Adherence
**Conclusion: Partial Pass**

**Rationale:** The core business objective (bus operations coordination platform with three roles) is correctly implemented. However, several explicit prompt requirements are either missing or incorrectly implemented:

**Correctly implemented constraints:**
- Local LAN deployment (no external service dependencies)
- PostgreSQL-only storage
- Password ≥8 with salted hashing (BCrypt)
- Message desensitization via redaction rules
- RESTful APIs decoupled from frontend
- Structured logging with trace IDs

**Missing/incorrect constraints:**
- **Pinyin/initial letter matching** — Prompt explicitly requires this; search regex at `SearchService.java:27` only accepts ASCII
- **DND window management** — Prompt requires "do not disturb periods (e.g., no notifications from 22:00 to 07:00)"; table exists (`V6__passenger_messaging.sql:38-47`) but no API endpoints
- **Default 10-minute reminder** — Prompt says "default 10 minutes in advance"; code defaults to 15 (`PassengerService.java:165`, `V6__passenger_messaging.sql:34`)
- **Message queue consumption** — Prompt says "triggered by local scheduled tasks"; no `@Scheduled` annotation exists anywhere in backend
- **Timeout escalation** — Prompt says "tasks unprocessed for 24 hours trigger escalation warnings"; no scheduled escalation mechanism
- **Conditional branching / joint-parallel approvals** — Prompt says "conditional branching, joint/parallel approvals"; workflow model is linear (approve/reject/return only)
- **HTML template parsing** — Prompt says "structured parsing of HTML/JSON templates"; only JSON import implemented
- **Cleaning rule application during import** — Prompt says fields "undergo standardized cleaning"; cleaning rules exist as CRUD but are not applied in the import pipeline

---

### Section 6: Aesthetics

#### 6.1 Visual and Interaction Design
**Conclusion: Partial Pass**

**Rationale:** The frontend provides functional role-based UIs with:

**Positive aspects:**
- Tab-based navigation within each role page for feature organization
- Consistent design language across all three role views (same font, spacing, table styling)
- Status badges with color-coded states (green for approved/completed, red for rejected, yellow for pending): `dispatcher.component.scss:96-103`
- Unread message visual indicator (blue left border): `passenger.component.scss:135`
- Login form with loading state and error feedback: `login.component.ts:23-41`
- Role-based navigation links adapt per user role: `passenger.component.html:5-10`, `dispatcher.component.html:5-10`
- Responsive flex layouts with wrapping: `dispatcher.component.scss:9-15`

**Negative aspects:**
- Functional but minimal visual design — no color theme, no branding beyond "City Bus" text
- No icons or visual hierarchy beyond headings and tables
- Input fields lack polish (no focus ring customization, no placeholder styling)
- No hover states on table rows
- No transitions or animations
- Admin page has 8 tabs which may be difficult to navigate on smaller screens: `admin.component.html:11-19`
- `system-ui, sans-serif` font stack lacks visual distinction

---

## 5. Issues / Suggestions (Severity-Rated)

### Issue #1 — Actuator endpoints publicly accessible including `/actuator/env`
**Severity: High**

**Conclusion:** All Actuator endpoints are permitted without authentication.

**Evidence:** `SecurityConfig.java:37` — `.requestMatchers("/actuator/**", "/error").permitAll()` combined with `application.yml:38-39` exposing `health,info,metrics,flyway,env`.

**Impact:** `/actuator/env` exposes environment variable keys and structure. While Spring Boot 3.x sanitizes sensitive values by default, it still reveals configuration keys, bean metadata, and system properties. `/actuator/metrics` exposes internal performance data. `/actuator/flyway` reveals database migration history. In a local LAN deployment, this increases the attack surface.

**Minimum actionable fix:** Restrict Actuator access to ADMIN role:
```java
.requestMatchers("/actuator/health").permitAll()
.requestMatchers("/actuator/**").hasRole("ADMIN")
```

---

### Issue #2 — No message queue processor (scheduled task consumer)
**Severity: High**

**Conclusion:** The `message_queue` and `message_queue_attempts` tables are populated on reservation/cancel events (`PassengerService.java:86`, `PassengerService.java:123`) but no consumer processes the queue.

**Evidence:**
- `message_queue` table defined at `V6__passenger_messaging.sql:62-71`
- `message_queue_attempts` table at `V6__passenger_messaging.sql:73-80`
- Grep for `@Scheduled` across entire backend: **zero matches**
- Grep for `@EnableScheduling`: **zero matches**
- No class references `MessageQueueRepository` for read/processing (only `save()` calls)

**Impact:** Notifications described in the prompt — "upcoming reminders (default 10 minutes in advance), and missed check-ins (5 minutes after start time)" — cannot be triggered. The message queue accumulates entries that are never processed. This is a core prompt requirement ("triggered by local scheduled tasks").

**Minimum actionable fix:** Add `@EnableScheduling` to `CityBusApplication` and create a `@Scheduled` service that polls `message_queue` for `QUEUED` entries, processes them, and records attempts in `message_queue_attempts`.

---

### Issue #3 — No DND (Do Not Disturb) window management API
**Severity: High**

**Conclusion:** The prompt explicitly requires "do not disturb periods (e.g., no notifications from 22:00 to 07:00)." The table exists but no API endpoints manage DND windows.

**Evidence:**
- `do_not_disturb_windows` table: `V6__passenger_messaging.sql:38-47`
- Entity and repository exist: `DoNotDisturbWindowEntity.java`, `DoNotDisturbWindowRepository.java`
- `PassengerController.java` has no DND-related endpoints
- `PassengerService.java` has no DND-related methods
- Frontend `passenger.component.html` has no DND section

**Impact:** Passengers cannot configure DND windows. Even if the queue processor existed, there would be no DND data to check.

**Minimum actionable fix:** Add CRUD endpoints for DND windows under `/api/v1/passenger/dnd-windows` and integrate DND checking into the notification processor.

---

### Issue #4 — No pinyin/initial letter matching in search
**Severity: High**

**Conclusion:** The prompt explicitly requires "pinyin/initial letter matching." The search implementation only supports ASCII text matching.

**Evidence:** `SearchService.java:27` — `Pattern.compile("^[a-zA-Z0-9\\s\\-]{2,128}$")` rejects all non-ASCII input. The ILIKE queries at lines 40-41 and 57-58 perform plain substring matching only.

**Impact:** A core search feature from the prompt is entirely missing. No Chinese character input or pinyin-to-character conversion is supported.

**Minimum actionable fix:** Add a pinyin library (e.g., pinyin4j) and extend the search to match against pinyin representations of stop/route names.

---

### Issue #5 — No workflow timeout escalation
**Severity: High**

**Conclusion:** The prompt requires "timeout alerts (tasks unprocessed for 24 hours trigger escalation warnings)." No scheduled mechanism exists.

**Evidence:**
- `workflow_escalations` table exists: `V7__workflow.sql:52-59`
- `WorkflowEscalationEntity.java` and `WorkflowEscalationRepository.java` exist
- No `@Scheduled` annotation anywhere in the codebase
- No code writes to `workflow_escalations` table
- No code checks task age against a 24-hour threshold

**Impact:** Tasks can remain pending indefinitely with no escalation alert.

**Minimum actionable fix:** Add a `@Scheduled(fixedRate = 3600000)` method that queries `workflow_tasks` for pending tasks older than 24 hours and creates escalation records + system alerts.

---

### Issue #6 — Cleaning rules not applied during data import
**Severity: Medium**

**Conclusion:** Cleaning rules exist as CRUD entities but are never applied to imported data.

**Evidence:**
- `CanonicalImportService.java:51-67` — calls `transactionalImportService.importCanonical()` without referencing cleaning rules
- `CleaningRuleSetRepository` is not injected into any ingestion service
- `cleaning_audit_logs` table is never populated by import processing

**Impact:** The prompt requires "fields such as stop names, addresses... undergo standardized cleaning" and "cleaning rules are configurable, with audit logs retained." Cleaning rules can be created via admin UI but have no effect on data.

**Minimum actionable fix:** Inject `CleaningRuleSetRepository` into the import service. Apply enabled rules during ingestion and record audit logs.

---

### Issue #7 — No HTML template parsing for data integration
**Severity: Medium**

**Conclusion:** The prompt requires "structured parsing of HTML/JSON templates." Only JSON import is implemented.

**Evidence:** `CanonicalImportService.java:51-67` accepts `CanonicalImportRequest` which is a JSON payload. No HTML parsing library is present in `pom.xml`. No HTML parsing class exists in the codebase.

**Impact:** One of the two explicitly stated data source formats is unsupported.

**Minimum actionable fix:** Add Jsoup or equivalent HTML parser dependency and create an HTML import variant alongside the existing JSON import.

---

### Issue #8 — No conditional branching or joint/parallel approval in workflow
**Severity: Medium**

**Conclusion:** The prompt requires "conditional branching, joint/parallel approvals." The workflow model is strictly linear.

**Evidence:**
- `workflow_definitions` has no branching fields: `V7__workflow.sql:3-10`
- `workflow_tasks` has no dependency/predecessor fields: `V7__workflow.sql:34-49`
- `WorkflowService.java:158-171` — recalculation is simple: any rejected → REJECTED, all decided → COMPLETED
- No concept of parallel approval paths or conditional routing between tasks

**Impact:** The workflow engine cannot model the approval flows described in the prompt.

**Minimum actionable fix:** Add `task_type` (sequential/parallel), `depends_on` fields to `workflow_tasks`, and branching conditions to workflow definitions.

---

### Issue #9 — Default reminder advance time is 15 minutes, prompt says 10
**Severity: Medium**

**Conclusion:** The prompt specifies "upcoming reminders (default 10 minutes in advance)." The implementation defaults to 15 minutes.

**Evidence:**
- `V6__passenger_messaging.sql:34` — `minutes_before INT NOT NULL DEFAULT 15`
- `PassengerService.java:165` — `new ReminderPreferenceResponse(true, 15, "IN_APP")`

**Impact:** Explicit prompt requirement not followed.

**Minimum actionable fix:** Change both defaults from 15 to 10.

---

### Issue #10 — Frontend sends `aliases` as array, backend expects String
**Severity: Medium**

**Conclusion:** Type mismatch between frontend dictionary creation and backend DTO.

**Evidence:**
- Frontend `admin.component.ts:127-130`:
  ```typescript
  const aliases = this.dictAliases ? this.dictAliases.split(',').map((s: string) => s.trim()) : [];
  this.http.post('/api/v1/admin/dictionaries', { ..., aliases, ... })
  ```
  Sends `aliases` as `string[]` (e.g., `["exp", "express"]`)
- Backend `SaveDictionaryEntryRequest.java:14`: `String aliases` — expects a plain String

**Impact:** When creating a dictionary entry via the admin UI, Jackson may serialize the array as `"[\"exp\",\"express\"]"` or fail deserialization depending on configuration. The integration test passes because it sends a plain string (`AdminConsoleIT.java:117`).

**Minimum actionable fix:** Either change frontend to send `this.dictAliases` as-is (comma-separated string), or change backend DTO to accept `List<String>` and serialize for storage.

---

### Issue #11 — No notification template management
**Severity: Medium**

**Conclusion:** The prompt says "Administrators maintain notification templates." The admin `GET /api/v1/admin/templates` endpoint returns field mappings (import templates), not notification templates.

**Evidence:**
- `AdminConfigController.java:41-44` — `listTemplates()` calls `adminConfigService.listTemplates()`
- `AdminConfigService.java:175-180` — returns `FieldMappingEntity` records (import field mappings)
- No `NotificationTemplate` entity, table, or service exists

**Impact:** Notification template management described in the prompt is not implemented. The `/templates` endpoint serves a different purpose (import field mappings).

**Minimum actionable fix:** Create a `notification_templates` table, entity, repository, and CRUD endpoints under `/api/v1/admin/notification-templates`.

---

### Issue #12 — No auto-triggered P95/queue backlog alerts
**Severity: Low**

**Conclusion:** The prompt requires "Queue backlogs and API P95 response times exceeding 500ms trigger local alerts and diagnostic reports." Only manual alert creation and diagnostics exist.

**Evidence:**
- `OperationsController.java:40-43` — `createAlert()` is manually triggered
- No code monitors API response times
- No code monitors `message_queue` backlog size
- No integration with Spring Boot Actuator metrics for automated alerting

**Impact:** Operational alerting is manual-only. The prompt describes automatic threshold-based alerts.

**Minimum actionable fix:** Add a `@Scheduled` service that periodically checks queue depth and Actuator metrics, creating `SystemAlertEntity` records when thresholds are exceeded.

---

### Issue #13 — Hardcoded JWT secret in docker-compose default
**Severity: Low**

**Conclusion:** A deterministic default JWT secret is used when `APP_JWT_SECRET` is not set.

**Evidence:**
- `docker-compose.yml:29` — `APP_JWT_SECRET: ${APP_JWT_SECRET:-0123456789abcdef...}`
- `application.yml:53` — same default
- README.md:48 notes "set a strong value in production"

**Impact:** For a local LAN deployment (per prompt), this is a low risk since the README explicitly warns about it. However, if deployed without reading docs, all tokens are predictable.

**Minimum actionable fix:** Already mitigated by documentation. Optionally, fail startup if the default secret is detected in non-dev profiles.

---

## 6. Security Review Summary

### Authentication entry points
**Conclusion: Pass**

- `POST /api/v1/auth/login` — `AuthController.java:23-26`
- Password validation ≥8 chars: `LoginRequest.java:7-8`
- BCrypt password hashing: `PasswordConfig.java` (BCrypt encoder bean)
- Login audit recording: `AuthService.java:44,49,53,59`
- Disabled user check: `AuthService.java:48-51`
- JWT with HMAC-SHA, configurable expiration: `JwtService.java:29-38`
- Minimum 256-bit key enforcement: `JwtService.java:23-24`

### Route-level authorization
**Conclusion: Pass**

- `SecurityConfig.java:35-44` — all `anyRequest().authenticated()` except login, actuator, and OPTIONS
- `@PreAuthorize` on every controller method with appropriate role checks:
  - Admin endpoints: `@PreAuthorize("hasRole('ADMIN')")` — `AdminConfigController.java:30`, `OperationsController.java:23`, `AdminImportController.java:28`
  - Workflow endpoints: `@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")` — `WorkflowController.java:30`, `TaskController.java:33`
  - Passenger endpoints: `@PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")` — `PassengerController.java:35`
  - Search/messages: `@PreAuthorize("isAuthenticated()")` — `SearchController.java:23`, `MessageController.java:29`
- `@EnableMethodSecurity` at `SecurityConfig.java:18`

### Object-level authorization
**Conclusion: Pass**

- Reservation ownership check: `PassengerService.java:108-109` — `if (!reservation.getUser().getId().equals(user.getId()))`
- Check-in reservation ownership: `PassengerService.java:139-141`
- Message user isolation: `MessageService.java:50` — `findByIdAndUser_Id(id, user.getId())`
- Reservation listing scoped to user: `PassengerService.java:94` — `findByUser_IdOrderByReservedAtDesc(user.getId())`
- Check-in listing scoped to user: `PassengerService.java:152`
- Message listing scoped to user: `MessageService.java:42`

### Function-level authorization
**Conclusion: Pass**

- Every controller uses `@PreAuthorize` with appropriate role checks (see route-level above)
- Class-level `@PreAuthorize("hasRole('ADMIN')")` on `AdminConfigController` and `OperationsController`
- Integration tests verify role enforcement:
  - `AdminConsoleIT.java:62-68` — dispatcher gets 403 on admin endpoints
  - `WorkflowIT.java:67-73` — passenger gets 403 on workflows
  - `PassengerMessagingIT.java:309-318` — dispatcher gets 403 on passenger reservations
  - `TransitCanonicalImportIT.java:93-103` — passenger gets 403 on import

### Tenant / user data isolation
**Conclusion: Pass**

All user-scoped data (reservations, check-ins, messages, reminder preferences) is filtered by the authenticated user's ID via `currentUser()` methods in services:
- `PassengerService.java:180-184`
- `MessageService.java:83-87`

### Admin / internal / debug endpoint protection
**Conclusion: Partial Pass**

- Admin CRUD endpoints properly protected with `@PreAuthorize("hasRole('ADMIN')")`: all `AdminConfigController`, `OperationsController`, `AdminImportController` methods
- **Actuator endpoints are unprotected** (see Issue #1): `SecurityConfig.java:37` — `/actuator/**` is `permitAll()`
- `RoleDemoController.java` exists as a demo endpoint — low risk but adds unnecessary attack surface

---

## 7. Tests and Logging Review

### Unit tests
**Conclusion: Fail**

No unit tests exist. All test classes are integration tests (`*IT.java`) using `@SpringBootTest` with full Spring context and Testcontainers.

**Evidence:** All 7 test files in `backend/src/test/java/` are `@SpringBootTest` integration tests. No files match `*Test.java` (unit test convention). The frontend has component-level specs using Karma/Jasmine (`*.spec.ts`).

**Impact:** Individual service logic (e.g., `textMatchScore`, ranking algorithm, redaction logic, workflow status recalculation) is only tested through HTTP round-trips, not in isolation.

### API / integration tests
**Conclusion: Pass**

Comprehensive integration test coverage across 7 test classes with 47 `@Test` methods:
- `CityBusApplicationIT` (9 tests) — auth, RBAC, health
- `TransitCanonicalImportIT` (2 tests) — import, role restriction
- `SearchApiIT` (5 tests) — suggestions, results, validation, impressions
- `PassengerMessagingIT` (11 tests) — reservations, check-ins, messages, preferences
- `WorkflowIT` (9 tests) — create, approve/reject/return, batch, conflict
- `AdminConsoleIT` (8 tests) — CRUD operations, role restriction
- `ObservabilityIT` (10 tests) — health, alerts, diagnostics, trace headers

**Evidence:** All use `@SpringBootTest(RANDOM_PORT)`, `@Testcontainers`, `TestRestTemplate`, `@ActiveProfiles("test")`. Test framework: JUnit 5 + AssertJ + Testcontainers + Spring Boot Test.

Frontend specs exist for all 3 feature components + auth module (5 spec files):
- `admin.component.spec.ts` (511 lines)
- `dispatcher.component.spec.ts` (237 lines)
- `passenger.component.spec.ts` (352 lines)
- `auth.service.spec.ts`, `auth.guard.spec.ts`, `auth.interceptor.spec.ts`, `role.guard.spec.ts`, `login.component.spec.ts`

### Logging categories / observability
**Conclusion: Pass**

- Structured logging pattern with trace ID: `application.yml:59-60`
- MDC-based trace ID propagation: `TraceIdFilter.java:29`
- `X-Trace-Id` response header: `TraceIdFilter.java:30`
- Log levels configured for root and Spring Web: `application.yml:56-58`
- Actuator health with DB and disk components: `application.yml:43-48`

### Sensitive-data leakage risk in logs / responses
**Conclusion: Partial Pass**

**Positive:**
- Password hash never exposed in responses (no `passwordHash` field in any response DTO)
- Message redaction rules applied before storing: `MessageService.java:68`
- Login error message is generic ("Invalid credentials"): `AuthService.java:45,50,54`
- BCrypt hashes stored, never logged in application code

**Concern:**
- Actuator `/actuator/env` publicly accessible (Issue #1) — exposes configuration structure
- `AuditLogResponse` includes `ipAddress`: `AdminConfigService.java:221` — acceptable for admin-only endpoint
- No explicit check that sensitive fields in request bodies aren't logged by Spring default request logging (low risk at `INFO` level)

---

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview

| Aspect | Detail |
|---|---|
| Unit tests | None (backend). Component specs exist (frontend). |
| Integration tests | 7 classes, 47 `@Test` methods |
| Test framework | JUnit 5, AssertJ, Testcontainers (PostgreSQL 16), Spring Boot Test, TestRestTemplate |
| Frontend test framework | Karma, Jasmine, `HttpTestingController` |
| Test entry point | `backend/pom.xml` Maven Surefire/Failsafe, `run_test.sh`, `scripts/mvn-verify-docker.sh` |
| Test commands documented | `README.md:83-103` |
| Test config | `application-test.yml:1-14` — `validate` DDL, test JWT secret |

### 8.2 Coverage Mapping Table

| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion | Coverage Assessment | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| Login happy path | `CityBusApplicationIT.loginSuccessAndPingWithBearer:73-90` | 200, `accessToken`, `X-Trace-Id` | Sufficient | — | — |
| Login bad password → 401 | `CityBusApplicationIT.loginBadPasswordReturns401:93-100` | 401 | Sufficient | — | — |
| Login short password → 400 | `CityBusApplicationIT.loginShortPasswordReturns400:103-110` | 400 | Sufficient | — | — |
| Auth/me endpoint | `CityBusApplicationIT.meReturnsUserAndRoles:113-124` | username, roles | Sufficient | — | — |
| Unauthenticated → 401 | `CityBusApplicationIT.apiPingRequiresAuthentication:69-72`, + 4 more across test classes | 401 on multiple endpoints | Sufficient | — | — |
| RBAC admin-only → 403 | `AdminConsoleIT.nonAdminCannotAccessAdminEndpoints:63-69`, `TransitCanonicalImportIT.passengerCannotRunImport:92-103` | 403 | Sufficient | — | — |
| RBAC dispatcher workflow | `WorkflowIT.passengerCannotAccessWorkflows:67-73` | 403 for passenger | Sufficient | — | — |
| RBAC passenger-only | `PassengerMessagingIT.dispatcherCannotAccessPassengerReservations:309-318` | 403 for dispatcher | Sufficient | — | — |
| JSON import + query | `TransitCanonicalImportIT.adminCanImportCanonicalJsonAndQueryRoutes:54-90` | SUCCEEDED, routes, stops | Sufficient | — | — |
| Search suggestions | `SearchApiIT.suggestionsReturnRoutesAndStops:100-119` | Non-empty, search events logged | Basically covered | No test for empty results | Add zero-result test |
| Search query validation | `SearchApiIT.shortQueryReturns400:87-97` | 400 | Basically covered | No test for special chars | Add special char test |
| Search impression tracking | `SearchApiIT.resultsIncrementImpressionsAndLogSearchEvent:122-143` | impression_count=1 | Sufficient | — | — |
| Reservation CRUD | `PassengerMessagingIT.passengerCanCreateAndListReservation:87-105`, `passengerCanCancelReservation:108-125` | 201, PENDING, CANCELLED | Sufficient | — | — |
| Check-in | `PassengerMessagingIT.passengerCanCheckin:128-145` | 201, stopCode | Sufficient | — | — |
| Reminder prefs default + update | `PassengerMessagingIT.reminderPreferencesDefaultValues:171-182`, `passengerCanUpdateReminderPreferences:185-207` | Default values, updated values | Sufficient | — | — |
| Reservation → message queue | `PassengerMessagingIT.reservationCreatesMessage:210-224` | message_queue count ≥ 1 | Basically covered | No queue processing test | Cannot test (no processor) |
| Message mark-read | `PassengerMessagingIT.messageMarkReadFlow:227-252` | read=true | Sufficient | — | — |
| Workflow create + tasks | `WorkflowIT.dispatcherCanCreateWorkflowAndTasks:76-96` | OPEN, PENDING | Sufficient | — | — |
| Approve/reject/return flow | `WorkflowIT.approveRejectReturnFlow:99-129` | RETURNED→APPROVED→COMPLETED | Sufficient | — | — |
| Reject → workflow rejected | `WorkflowIT.rejectTaskSetsWorkflowRejected:132-145` | REJECTED | Sufficient | — | — |
| Double decision → 409 | `WorkflowIT.doubleDecisionReturnsConflict:148-163` | 409 CONFLICT | Sufficient | — | — |
| Batch task processing | `WorkflowIT.batchApprove:166-181` | processed=2 | Sufficient | — | — |
| Admin cleaning rules CRUD | `AdminConsoleIT.cleaningRuleCrud:72-107` | CREATE/GET/PUT/DELETE | Sufficient | — | — |
| Admin dictionary CRUD | `AdminConsoleIT.dictionaryCrud:110-146` | CREATE/GET/PUT/DELETE | Sufficient | — | — |
| Admin ranking config | `AdminConsoleIT.rankingConfigGetAndUpdate:149-170` | GET/PUT, maxResults=25 | Sufficient | — | — |
| Admin user enable/disable | `AdminConsoleIT.usersListAndUpdate:184-213` | disable→enable | Sufficient | — | — |
| Alerts create/ack/list | `ObservabilityIT.alertCreateListAcknowledge:79-112` | WARN, acknowledged | Sufficient | — | — |
| Double ack → 409 | `ObservabilityIT.doubleAcknowledgeReturnsConflict:115-134` | 409 | Sufficient | — | — |
| Diagnostics DB_HEALTH | `ObservabilityIT.diagnosticDbHealth:137-148` | COMPLETED, "Database reachable" | Sufficient | — | — |
| Diagnostics FULL | `ObservabilityIT.diagnosticFull:159-169` | COMPLETED, contains DB_HEALTH | Sufficient | — | — |
| Health + trace header | `ObservabilityIT.healthEndpointPubliclyAccessible:53-57`, `traceIdHeaderReturned:65-68` | UP, X-Trace-Id | Sufficient | — | — |
| DND window management | None | — | **Missing** | No API exists | Implement API then test |
| Queue processing | None | — | **Missing** | No processor exists | Implement processor then test |
| Timeout escalation | None | — | **Missing** | No escalation code exists | Implement then test |
| Object-level auth on reservations | `PassengerMessagingIT.passengerCanCancelReservation` (implicit — only tests own user) | — | **Insufficient** | No cross-user access test | Add test: user A cannot cancel user B's reservation |
| Object-level auth on messages | Not directly tested (all tests use single user) | — | **Insufficient** | No cross-user access test | Add test: user A cannot read user B's message |

### 8.3 Security Coverage Audit

| Security dimension | Test coverage | Assessment |
|---|---|---|
| Authentication (401) | `CityBusApplicationIT:69-72`, `AdminConsoleIT:52-57`, `WorkflowIT:62-65`, `PassengerMessagingIT:84-86`, `SearchApiIT:82-85`, `ObservabilityIT:70-76` | **Sufficient** — every endpoint group tested for unauthenticated access |
| Route authorization (403) | `AdminConsoleIT:62-69`, `WorkflowIT:67-73`, `PassengerMessagingIT:309-318`, `TransitCanonicalImportIT:92-103`, `CityBusApplicationIT:127-137` | **Sufficient** — cross-role access tested for admin, dispatcher, passenger |
| Object-level authorization | Implicit only (all tests use single user per role) | **Insufficient** — no test verifies that user A cannot access user B's reservations, messages, or check-ins |
| Tenant / data isolation | Implicit only | **Insufficient** — same gap as object-level auth |
| Admin / internal protection | `AdminConsoleIT.nonAdminCannotAccessAdminEndpoints:62-69`, `ObservabilityIT.alertsRequireAdmin:70-76` | **Basically covered** — dispatcher tested against admin endpoints, but actuator endpoints are untested for security |

### 8.4 Final Coverage Judgment

**Conclusion: Partial Pass**

**Covered major risks:**
- Authentication flows (login, bad credentials, short password, token verification)
- Role-based access control across all three roles
- Core CRUD happy paths for all implemented features
- Workflow state machine transitions (approve, reject, return, batch, conflict)
- Data import with verification
- Search with ranking and impression tracking
- Observability endpoints and headers

**Uncovered risks that could hide severe defects:**
- No cross-user object-level authorization tests — a bug allowing user A to read/modify user B's data would go undetected
- No tests for missing features (DND windows, queue processing, escalation)
- No unit tests for business logic edge cases (ranking algorithm, redaction regex, workflow recalculation)
- No test for actuator endpoint security (metrics/env exposure)
- No frontend E2E tests (only component-level specs with mocked HTTP)

---

## 9. Final Notes

This delivery represents a substantial engineering effort with a well-organized codebase, comprehensive documentation, and meaningful integration test coverage. The architecture is sound for the described use case (local LAN Spring Boot + Angular + PostgreSQL).

The primary gaps are:
1. **Several explicitly stated prompt features are missing** (DND management, queue processing, timeout escalation, pinyin search, HTML parsing, cleaning integration, conditional branching)
2. **Actuator security configuration** exposes potentially sensitive endpoints
3. **No cross-user authorization tests** leave a significant security verification gap

The missing features are not stubs or placeholders — they are entirely absent from the codebase (though some have the database tables created). This indicates the implementation covered breadth well but did not achieve full depth on several prompt requirements.

The codebase is professional in structure and would be maintainable for a team to extend. The issues identified are primarily scope/completeness gaps rather than fundamental architectural flaws.
