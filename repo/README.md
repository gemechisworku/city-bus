# City Bus — Operation and Service Coordination Platform

Docker-first **Angular + Spring Boot + PostgreSQL** stack. Treat **this directory** (`repo/`) as the project root for Compose, scripts, and development unless your hosting layout nests it differently.

## Stack versions

| Layer | Version |
|-------|---------|
| Angular | 19.x |
| Spring Boot | 3.4.x |
| Java (runtime) | 17 |
| PostgreSQL | 16.x |

## Layout

| Path | Purpose |
|------|---------|
| `backend/` | Spring Boot API, Flyway migrations, tests |
| `frontend/` | Angular workspace |
| `samples/integration/` | Sample import sources (Phase 3+) |
| `docs/` | ADR, API outline, ERD reference, acceptance checklist |
| `docker-compose.yml` | Postgres + API + nginx UI |
| `run_test.sh` | Backend `mvnw verify` + frontend `npm run test:ci` (expects JDK + Node on the host) |
| `scripts/mvn-verify-docker.ps1` / `scripts/mvn-verify-docker.sh` | Backend `mvnw verify` inside **eclipse-temurin:17-jdk** with a **persistent Maven cache** volume and Docker socket (no JDK on the host) |

## Prerequisites

- **Docker** and **Docker Compose** for `docker compose up --build`.
- **JDK 17+** only if you run `./backend/mvnw` on the host (optional if you use the Docker-backed script below).
- **Node.js 22+** and **npm** for the frontend and for `run_test.sh` (frontend step).

## Configuration

Copy `.env.example` to `.env`. Defaults:

- PostgreSQL: `citybus` / `citybus`, port **5432**
- Backend: **8080**
- Frontend (nginx): **80**
- **JWT:** `APP_JWT_SECRET` (≥ 32 bytes; set a strong value in production) and optional `APP_JWT_EXPIRATION_SECONDS`

## Start

```bash
docker compose up --build
```

| Resource | URL |
|----------|-----|
| UI (login) | http://localhost/login |
| Auth login API | `POST http://localhost/api/v1/auth/login` |
| Health (via nginx) | http://localhost/actuator/health |
| Health (direct API) | http://localhost:8080/actuator/health |

Protected JSON APIs require `Authorization: Bearer <JWT>` (see `docs/api-outline.md`). Example: `GET /api/v1/ping` after login.

### Local frontend dev (`ng serve`)

From `frontend/`, `ng serve` uses `proxy.conf.json` to forward `/api` and `/actuator` to `http://localhost:8080`. Start the backend first.

## Seed credentials (Flyway)

Password policy for login: **≥ 8 characters** (enforced on the API request body).

| Username | Password | Role |
|----------|------------|------|
| `admin` | `ChangeMe123!` | ADMIN |
| `dispatcher1` | `ChangeMe123!` | DISPATCHER |
| `passenger1` | `ChangeMe123!` | PASSENGER |

## Tests

**Backend only, no JDK on the host (recommended if you use Docker for tooling):** from this directory, a named volume `city-bus-maven-cache` stores Maven dependencies so repeat runs are fast. The Docker socket is mounted so **Testcontainers** can start PostgreSQL for integration tests.

```powershell
.\scripts\mvn-verify-docker.ps1
```

```bash
chmod +x scripts/mvn-verify-docker.sh
./scripts/mvn-verify-docker.sh
```

**Full suite (host JDK + Node):** backend and frontend tests.

```bash
chmod +x run_test.sh
./run_test.sh
```

Optional after Compose is up:

```bash
chmod +x scripts/smoke-compose.sh
./scripts/smoke-compose.sh http://localhost
```

## Implementation status

**Completed:** Phases **0–4** (through search, ranking, and stop detail API).

**Next:** **Phase 5** — passenger reservation, check-in, reminders, message center.

Details: [`docs/implementation-status.md`](docs/implementation-status.md).

## Monorepo note

If this tree lives inside a parent folder in git, run Compose and `run_test.sh` from **this** directory, or set CI `working-directory` to `repo` in a workflow at the parent repository root.
