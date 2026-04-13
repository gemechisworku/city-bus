# City Bus — Operation and Service Coordination Platform

Docker-first **Angular + Spring Boot + PostgreSQL** stack for passengers, dispatchers, and operations administrators. This README matches the current codebase (ports, URLs, credentials, and commands).

## Stack versions

| Layer | Version |
|-------|---------|
| Angular | 19.x |
| Spring Boot | 3.4.x |
| Java (runtime) | 17 |
| PostgreSQL | 16.x |

## Repository layout

| Path | Purpose |
|------|---------|
| `backend/` | Spring Boot API, Flyway migrations, tests |
| `frontend/` | Angular workspace |
| `samples/integration/` | Sample import sources (Phase 3+) |
| `docs/` | ADR, API outline, ERD reference, acceptance checklist |
| `plans/implementation_plan.md` | Delivery phases and constraints |
| `docker-compose.yml` | Postgres + API + static UI with reverse proxy |
| `run_test.sh` | Backend `mvnw verify` + frontend `npm run test:ci` |

## Prerequisites

- **Docker** and **Docker Compose** for local runtime (`docker compose up --build`).
- **JDK 17+** on the host if you run `./mvnw` outside Docker (the aggregate test script uses `backend/mvnw`).
- **Node.js 22+** and **npm** for frontend development and tests.

## Configuration

Copy `.env.example` to `.env` and adjust. Compose maps:

- **PostgreSQL:** host port `POSTGRES_PORT` (default **5432**)
- **Backend:** `BACKEND_PORT` (default **8080**)
- **Frontend (nginx):** `FRONTEND_PORT` (default **80**)

Database name, user, and password default to `citybus` / `citybus` unless overridden in `.env`.

## Start (Docker)

From the repository root:

```bash
docker compose up --build
```

Wait until Postgres is healthy and the backend has finished Flyway migrations, then:

| Resource | URL |
|----------|-----|
| Angular UI (via nginx) | http://localhost/ |
| API prefix | http://localhost/api/v1/ |
| Example | http://localhost/api/v1/ping |
| Actuator health (direct API port) | http://localhost:8080/actuator/health |
| Actuator health (via nginx) | http://localhost/actuator/health |

## API

- Versioned REST base: **`/api/v1`** (see `docs/api-outline.md`).
- Phase 1 exposes **`GET /api/v1/ping`** and actuator endpoints; authentication APIs arrive in Phase 2.

## Seed data (database)

Flyway applies `V1__auth_tables.sql` and `V2__seed_roles_and_admin.sql`.

| Field | Value |
|-------|--------|
| Username | `admin` |
| Password | `ChangeMe123!` |
| Role | `ADMIN` |

Change this password before production use.

## Health

- **`GET /actuator/health`** — Spring Boot actuator (UP when the app and datasource are healthy).

## Tests

From the repository root:

```bash
chmod +x run_test.sh
./run_test.sh
```

Runs:

1. `backend`: `./mvnw -B verify` (includes Testcontainers PostgreSQL integration tests).
2. `frontend`: `npm ci` and `npm run test:ci` (Karma, ChromeHeadless).

CI runs the same steps via `.github/workflows/ci.yml`.

### Optional Compose smoke

After `docker compose up -d --build`:

```bash
chmod +x scripts/smoke-compose.sh
./scripts/smoke-compose.sh http://localhost
```

## Implementation status

Phases **0–1** (inception + foundation) are delivered: documentation, ADR, Docker Compose, Spring Boot with Flyway auth schema and seed, Angular shell, trace ID filter (`X-Trace-Id`), and tests above. Authentication endpoints and RBAC enforcement are **Phase 2**.

## Troubleshooting

- **Port conflicts:** set `POSTGRES_PORT`, `BACKEND_PORT`, or `FRONTEND_PORT` in `.env`.
- **Backend tests:** integration tests need Docker (Testcontainers). Ensure the Docker daemon is running.
- **Frontend tests:** require a Chrome-compatible browser for Karma (GitHub Actions `ubuntu-latest` includes dependencies for headless Chrome).
