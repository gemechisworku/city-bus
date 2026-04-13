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
| `run_test.sh` | Backend `mvnw verify` + frontend `npm run test:ci` |

## Prerequisites

- **Docker** and **Docker Compose** for `docker compose up --build`.
- **JDK 17+** for `./backend/mvnw` on the host.
- **Node.js 22+** and **npm** for the frontend.

## Configuration

Copy `.env.example` to `.env`. Defaults:

- PostgreSQL: `citybus` / `citybus`, port **5432**
- Backend: **8080**
- Frontend (nginx): **80**

## Start

```bash
docker compose up --build
```

| Resource | URL |
|----------|-----|
| UI | http://localhost/ |
| API example | http://localhost/api/v1/ping |
| Health (via nginx) | http://localhost/actuator/health |
| Health (direct API) | http://localhost:8080/actuator/health |

## Seed credentials (Flyway)

| Field | Value |
|-------|--------|
| Username | `admin` |
| Password | `ChangeMe123!` |
| Role | `ADMIN` |

## Tests

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

**Completed:** **Phase 0** (inception) and **Phase 1** (foundation and infrastructure).

**Next:** **Phase 2** — authentication and RBAC (login/logout/me, password policy, Angular guards, audit).

Details: [`docs/implementation-status.md`](docs/implementation-status.md).

## Monorepo note

If this tree lives inside a parent folder in git, run Compose and `run_test.sh` from **this** directory, or set CI `working-directory` to `repo` in a workflow at the parent repository root.
