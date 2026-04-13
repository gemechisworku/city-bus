# REST API outline (v1)

Base path: **`/api/v1`**. All JSON APIs use UTF-8. Clients should send `Accept: application/json` and `Content-Type: application/json` where applicable. Observability headers: **`X-Trace-Id`** (optional on request; echoed on response when generated server-side).

| Area | Methods | Examples |
|------|---------|----------|
| Auth | POST, GET | `/api/v1/auth/login`, `/api/v1/auth/logout`, `/api/v1/auth/me` |
| Search | GET | `/api/v1/search/suggestions?q=`, `/api/v1/search/results?...`, `/api/v1/routes/{id}`, `/api/v1/stops/{id}` |
| Passenger | POST, GET, PUT | `/api/v1/passenger/reservations`, `/api/v1/passenger/checkins`, `/api/v1/passenger/reminder-preferences` |
| Messages | GET, POST | `/api/v1/messages`, `/api/v1/messages/{id}`, `/api/v1/messages/{id}/read` |
| Workflow | GET, POST | `/api/v1/tasks`, `/api/v1/tasks/{id}/approve`, `/reject`, `/return`, `/api/v1/tasks/batch`, `/api/v1/workflows/{instanceId}` |
| Admin | CRUD, GET | `/api/v1/admin/templates`, `/ranking-config`, `/dictionaries`, `/cleaning-rules`, `/users`, `/api/v1/admin/audit` |
| Import / ops | POST, GET | `/api/v1/admin/imports/run`, `/api/v1/admin/imports`, `/api/v1/admin/alerts`, `/api/v1/admin/diagnostics` |

**Phase 3 (canonical bus data):** `POST /api/v1/admin/imports/run` (ADMIN) accepts JSON `DEFAULT_V1` payloads (see `samples/integration/canonical-routes.sample.json`). `GET /api/v1/admin/imports` lists import jobs. `GET /api/v1/routes` and `GET /api/v1/routes/{id}` expose the latest route version for authenticated users.

**Phase 4 (search):** `GET /api/v1/search/suggestions?q=&limit=` and `GET /api/v1/search/results?q=&limit=` return ranked `ROUTE` / `STOP` hits using `ranking_config` weights and `stop_popularity_metrics` (impressions updated when stops appear in **results**). `q` must be 2–128 characters: letters, digits, ASCII spaces, and hyphen only. `GET /api/v1/stops/{id}` returns the latest stop version.

**Auth (Phase 2):** `POST /api/v1/auth/login` accepts JSON `{ "username", "password" }` (password ≥ 8 characters). Response includes `accessToken` (JWT). Send `Authorization: Bearer <token>` for protected routes. `POST /api/v1/auth/logout` is idempotent for clients (stateless JWT). `GET /api/v1/auth/me` returns `userId`, `username`, `roles`. RBAC demo: `GET /api/v1/demo/admin` (ADMIN), `/demo/dispatcher` (ADMIN or DISPATCHER), `/demo/passenger` (ADMIN or PASSENGER).

**Phase 5 (passenger & messaging):** `POST /api/v1/passenger/reservations` (body: `{ "scheduleId", "stopId" }`; PASSENGER or ADMIN) creates a reservation and enqueues a notification. `GET /api/v1/passenger/reservations` lists the current user's reservations. `PUT /api/v1/passenger/reservations/{id}` (body: `{ "status": "CONFIRMED"|"CANCELLED" }`) updates status and enqueues a notification. `POST /api/v1/passenger/checkins` (body: `{ "stopId", "reservationId"? }`). `GET /api/v1/passenger/checkins` lists check-ins. `GET /api/v1/passenger/reminder-preferences` returns defaults or stored preferences. `PUT /api/v1/passenger/reminder-preferences` (body: `{ "enabled", "minutesBefore", "channel" }`). `GET /api/v1/messages` lists user's messages. `GET /api/v1/messages/{id}` returns a single message. `POST /api/v1/messages/{id}/read` marks a message as read. `POST /api/v1/messages` (body: `{ "subject", "body" }`) creates a message and enqueues it; redaction rules are applied to the body.

**Phase 0–1:** Actuator remains public; other `/api/v1/**` routes require a valid JWT from Phase 2 onward.

**Spring Boot Actuator** (not under `/api/v1`): `GET /actuator/health`, `GET /actuator/info`, metrics as configured.
