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

**Phase 0–1:** Public health and actuator endpoints are exposed for infrastructure checks; authenticated API routes are delivered in Phase 2+.

**Spring Boot Actuator** (not under `/api/v1`): `GET /actuator/health`, `GET /actuator/info`, metrics as configured.
