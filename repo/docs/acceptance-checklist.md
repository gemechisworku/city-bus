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
