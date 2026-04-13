# REST API outline (v1)

Base path: **`/api/v1`**. All JSON APIs use UTF-8. Clients should send `Accept: application/json` and `Content-Type: application/json` where applicable. Observability headers: **`X-Trace-Id`** (optional on request; echoed on response when generated server-side).

| Area | Methods | Examples |
|------|---------|----------|
| Auth | POST, GET | `/api/v1/auth/login`, `/api/v1/auth/logout`, `/api/v1/auth/me` |
| Search | GET | `/api/v1/search/suggestions?q=`, `/api/v1/search/results?...`, `/api/v1/routes/{id}`, `/api/v1/stops/{id}` |
| Passenger | POST, GET, PUT | `/api/v1/passenger/reservations`, `/api/v1/passenger/checkins`, `/api/v1/passenger/reminder-preferences` |
| Messages | GET, POST | `/api/v1/messages`, `/api/v1/messages/{id}`, `/api/v1/messages/{id}/read` |
| Workflow | GET, POST | `/api/v1/tasks`, `/api/v1/tasks/{id}/approve`, `/reject`, `/return`, `/api/v1/tasks/batch`, `/api/v1/workflows/{instanceId}` |
| Admin | CRUD, GET | `/api/v1/admin/templates`, `/notification-templates`, `/ranking-config`, `/dictionaries`, `/cleaning-rules`, `/users`, `/api/v1/admin/audit` |
| Import / ops | POST, GET | `/api/v1/admin/imports/run`, `/api/v1/admin/imports`, `/api/v1/admin/alerts`, `/api/v1/admin/diagnostics` |

**Phase 3 (canonical bus data):** `POST /api/v1/admin/imports/run` (ADMIN) accepts JSON `DEFAULT_V1` payloads (see `samples/integration/canonical-routes.sample.json`). `GET /api/v1/admin/imports` lists import jobs. `GET /api/v1/routes` and `GET /api/v1/routes/{id}` expose the latest route version for authenticated users.

**Phase 4 (search):** `GET /api/v1/search/suggestions?q=&limit=` and `GET /api/v1/search/results?q=&limit=` return ranked `ROUTE` / `STOP` hits using `ranking_config` weights and `stop_popularity_metrics` (impressions updated when stops appear in **results**). `q` must be 2–128 characters: letters, digits, ASCII spaces, and hyphen only. `GET /api/v1/stops/{id}` returns the latest stop version.

**Auth (Phase 2):** `POST /api/v1/auth/login` accepts JSON `{ "username", "password" }` (password ≥ 8 characters). Response includes `accessToken` (JWT). Send `Authorization: Bearer <token>` for protected routes. `POST /api/v1/auth/logout` is idempotent for clients (stateless JWT). `GET /api/v1/auth/me` returns `userId`, `username`, `roles`. Optional RBAC demo routes under `/api/v1/demo/*` exist only when `app.demo-endpoints-enabled=true` (off by default).

**Phase 5 (passenger & messaging):** `POST /api/v1/passenger/reservations` (body: `{ "scheduleId", "stopId" }`; PASSENGER or ADMIN) creates a reservation and enqueues a notification. `GET /api/v1/passenger/reservations` lists the current user's reservations. `PUT /api/v1/passenger/reservations/{id}` (body: `{ "status": "CONFIRMED"|"CANCELLED" }`) updates status and enqueues a notification. `POST /api/v1/passenger/checkins` (body: `{ "stopId", "reservationId"? }`). `GET /api/v1/passenger/checkins` lists check-ins. `GET /api/v1/passenger/reminder-preferences` returns defaults or stored preferences. `PUT /api/v1/passenger/reminder-preferences` (body: `{ "enabled", "minutesBefore", "channel" }`). `GET /api/v1/messages` lists user's messages. `GET /api/v1/messages/{id}` returns a single message. `POST /api/v1/messages/{id}/read` marks a message as read. `POST /api/v1/messages` (body: `{ "subject", "body" }`) creates a message and enqueues it; redaction rules are applied to the body.

**Phase 6 (workflow):** `POST /api/v1/workflows` (body: `{ "definitionId", "title" }`; ADMIN or DISPATCHER) creates a workflow instance. `GET /api/v1/workflows?status=` lists instances (with embedded tasks). `GET /api/v1/workflows/{instanceId}` returns one instance with tasks. `POST /api/v1/tasks` (body: `{ "instanceId", "title", "description"?, "assignedToUserId"?, "predecessorTaskId"? OR "predecessorTaskIds"? }`) adds a task; **all** listed predecessor tasks (same instance) must be **APPROVED** before this task can be decided (join / multi-way gating). `GET /api/v1/tasks?status=` lists tasks (each includes `instanceId`, `predecessorTaskIds`). `POST /api/v1/tasks/{id}/approve`, `.../reject`, `.../return` (body: `{ "note"? }`) decide a pending task. `POST /api/v1/tasks/batch` (body: `{ "taskIds", "action": "APPROVE"|"REJECT"|"RETURN", "note"? }`) batch-decides multiple tasks. Instance auto-transitions: all tasks approved → COMPLETED; any task rejected → REJECTED.

**Phase 7 (admin console):** `GET/POST/PUT/DELETE /api/v1/admin/cleaning-rules` — CRUD for regex-based data cleaning rules (ADMIN only). `GET/POST/PUT/DELETE /api/v1/admin/dictionaries` — CRUD for field standardisation dictionaries. `GET /api/v1/admin/ranking-config` — read current ranking weights. `PUT /api/v1/admin/ranking-config` (body: `{ "routeWeight", "stopWeight", "popularityWeight", "maxSuggestions", "maxResults" }`) — update ranking config. `GET /api/v1/admin/templates` — list **import** field mapping templates. `GET/POST/PUT/DELETE /api/v1/admin/notification-templates` — CRUD for **notification message** templates (subject/body/channel; distinct from import templates). `GET /api/v1/admin/users` — list all users. `GET /api/v1/admin/users/{id}` — user detail. `PUT /api/v1/admin/users/{id}` (body: `{ "enabled" }`) — enable/disable a user. `GET /api/v1/admin/audit` — login audit log (last 200 entries).

**Phase 8 (observability & operations):** `POST /api/v1/admin/alerts` (body: `{ "severity": "INFO"|"WARN"|"ERROR"|"CRITICAL", "source", "title", "detail"? }`; ADMIN) creates a system alert. `GET /api/v1/admin/alerts?unacknowledged=` lists alerts. `POST /api/v1/admin/alerts/{id}/acknowledge` marks an alert as acknowledged. `POST /api/v1/admin/diagnostics` (body: `{ "reportType": "DB_HEALTH"|"TABLE_STATS"|"CONNECTION_POOL"|"FULL" }`) runs a diagnostic and returns the report. `GET /api/v1/admin/diagnostics` lists historical reports.

**Phase 0–1:** `GET /actuator/health` and `GET /actuator/info` are **unauthenticated**; other `/actuator/**` endpoints (e.g. metrics, flyway) require **ADMIN**. All `/api/v1/**` routes except `POST /api/v1/auth/login` require a valid JWT.

**Spring Boot Actuator** (not under `/api/v1`): `GET /actuator/health`, `GET /actuator/info` (public); `GET /actuator/metrics`, `GET /actuator/flyway` (**ADMIN**). **`/actuator/env` is not exposed** by default in `application.yml`.

**RBAC demo (`/api/v1/demo/*`):** disabled in default configuration (`app.demo-endpoints-enabled=false`); enable only in controlled environments if needed.
