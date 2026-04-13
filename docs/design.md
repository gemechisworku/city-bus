# City Bus — System Design Document

## 1 System Overview

City Bus is an operation and service coordination platform for urban transit. It provides route management, passenger self-service, dispatcher workflow automation, and administrative control through a single deployable stack.

The system is delivered as a **Docker-first monorepo** containing three containers — an Angular 19 SPA served by nginx, a Spring Boot 3.4 REST API, and a PostgreSQL 16 database — orchestrated by Docker Compose. All infrastructure runs from a single `docker compose up --build` command with no external dependencies beyond Docker.

### Functional scope

| Capability | Description |
|-----------|-------------|
| **Authentication & RBAC** | JWT-based login with three seeded roles (ADMIN, DISPATCHER, PASSENGER) and method-level authorization |
| **Transit data ingestion** | Canonical JSON import pipeline producing versioned routes, stops, and schedules |
| **Search & ranking** | Weighted text search with configurable ranking weights and stop-popularity tracking |
| **Passenger self-service** | Reservations, check-ins, reminder preferences, Do-Not-Disturb windows |
| **Messaging** | User message center with a PostgreSQL-backed delivery queue and configurable redaction rules |
| **Dispatcher workflows** | Workflow instance lifecycle with task creation, approval/rejection/return, batch decisions, and auto-status rollup |
| **Admin console** | Data cleaning rules, field standardization dictionaries, ranking tuning, user management, login audit |
| **Observability & operations** | System alerts, diagnostic reports (DB health, connection pool, table stats), enhanced Actuator endpoints, request tracing |
| **Backup & restore** | Scripted `pg_dump`/`pg_restore` for live Compose stacks (Bash + PowerShell) |

---

## 2 Design Goals

| Goal | Rationale |
|------|-----------|
| **Single-command deployment** | `docker compose up --build` must produce a fully working system with seed data; no host-only toolchains required for running the platform |
| **Modular monolith** | One Spring Boot artifact with clear package boundaries (auth, transit, search, passenger, messaging, workflow, admin, observability) that can be decomposed later without upfront microservice overhead (ADR-001) |
| **Schema-driven evolution** | Flyway migrations are the single source of truth for the relational schema; Hibernate runs in `validate` mode and never generates DDL |
| **Stateless API tier** | JWT authentication with no server-side session state; horizontal scaling requires only load-balancer affinity for in-flight requests, not sticky sessions |
| **Infrastructure simplicity** | PostgreSQL-backed internal queue instead of an external broker; avoids adding containers that cannot run reliably in the Compose topology (ADR-001) |
| **Role-based least privilege** | Every API endpoint enforces RBAC at the method level via `@PreAuthorize`; frontend guards mirror backend rules but are not the trust boundary |
| **Operational self-sufficiency** | Built-in diagnostics, alerting, audit logs, and backup/restore scripts so the platform can be operated without third-party tooling |
| **Reproducible testing** | Testcontainers spins up a real PostgreSQL instance for integration tests; no H2 or embedded substitutes |

---

## 3 High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Docker Compose                      │
│                                                          │
│  ┌──────────┐     ┌──────────────┐     ┌─────────────┐  │
│  │ frontend │────▶│   backend    │────▶│  postgres    │  │
│  │ (nginx)  │     │ (Spring Boot)│     │ (PostgreSQL) │  │
│  │  :80     │     │   :8080      │     │   :5432      │  │
│  └──────────┘     └──────────────┘     └─────────────┘  │
│       │                  │                    │           │
│   static SPA      REST /api/v1         Flyway migrations │
│   + reverse       + /actuator          + seed data       │
│     proxy                                                │
└─────────────────────────────────────────────────────────┘
```

### Container responsibilities

| Container | Base image | Role |
|-----------|-----------|------|
| **frontend** | `node:22-alpine` (build) → `nginx:1.27-alpine` (runtime) | Builds the Angular 19 SPA with `ng build --configuration=production`, serves static assets, and reverse-proxies `/api/` and `/actuator/` to the backend |
| **backend** | `maven:3-eclipse-temurin-17` (build) → `eclipse-temurin:17-jre-alpine` (runtime) | Spring Boot 3.4 application exposing REST endpoints, running Flyway migrations on startup, and managing JPA persistence to PostgreSQL |
| **postgres** | `postgres:16-alpine` | Relational store for all domain data; health-checked with `pg_isready` before the backend starts |

### Startup order and health gates

1. **postgres** starts and passes its `pg_isready` health check.
2. **backend** starts (`depends_on: postgres [service_healthy]`), Flyway applies pending migrations, Hikari pool connects, and the `/actuator/health` endpoint returns `UP`.
3. **frontend** starts (`depends_on: backend [service_healthy]`) and begins serving the SPA.

Graceful shutdown is configured with `server.shutdown=graceful` and a 30-second timeout. The backend Compose service has `stop_grace_period: 30s` to match.

### Network flow

- Browser → **nginx :80** → static files (Angular SPA)
- Browser → **nginx :80** `/api/*`, `/actuator/*` → reverse proxy → **backend :8080**
- **backend** → **postgres :5432** (JDBC over Hikari connection pool)

During local development, `ng serve` uses `proxy.conf.json` to forward `/api` and `/actuator` to `http://localhost:8080`, bypassing nginx entirely.

---

## 4 Frontend Architecture

### 4.1 Technology and bootstrap

The frontend is an **Angular 19** application using the **standalone component** model — there is no `NgModule`. The app bootstraps via `bootstrapApplication(AppComponent, appConfig)` in `main.ts`.

`appConfig` provides:
- `provideRouter(routes)` with lazy-loaded feature routes
- `provideHttpClient(withInterceptors([authInterceptor]))` for token attachment
- Zone-based change detection with event coalescing

### 4.2 Component tree

```
AppComponent (shell — <router-outlet />)
├── LoginComponent          /login
├── PassengerComponent      /passenger    (ADMIN, PASSENGER)
├── DispatcherComponent     /dispatcher   (ADMIN, DISPATCHER)
└── AdminComponent          /admin        (ADMIN)
```

All feature components are **standalone** and **lazy-loaded**. Each is a self-contained page with tab-based sub-navigation covering its domain's feature set.

### 4.3 Route definitions

| Path | Component | Guards | Allowed roles |
|------|-----------|--------|---------------|
| `/login` | `LoginComponent` | — | Public |
| `/passenger` | `PassengerComponent` | `authGuard`, `roleGuard` | ADMIN, PASSENGER |
| `/dispatcher` | `DispatcherComponent` | `authGuard`, `roleGuard` | ADMIN, DISPATCHER |
| `/admin` | `AdminComponent` | `authGuard`, `roleGuard` | ADMIN |
| `**` (wildcard) | redirect → `/login` | — | — |

Post-login routing sends ADMIN users to `/admin`, DISPATCHER users to `/dispatcher`, and all others to `/passenger`.

### 4.4 Auth flow (client side)

| Artifact | Type | Responsibility |
|----------|------|----------------|
| `AuthService` | `@Injectable` service | `login()`, `logout()`, `ensureUser()`, `hasAnyRole()`; stores JWT in `localStorage` under key `citybus.token`; exposes `user$` observable |
| `authGuard` | Functional `CanActivateFn` | Redirects to `/login` if no token or if `ensureUser()` fails |
| `roleGuard(allowedRoles)` | Factory → `CanActivateFn` | Calls `ensureUser()`, then checks `user.roles` against the allowed set |
| `authInterceptor` | Functional `HttpInterceptorFn` | Attaches `Authorization: Bearer <token>` to every request except `POST /api/v1/auth/login` |

### 4.5 Feature component API usage

Feature components inject `HttpClient` directly (no intermediate Angular service layer beyond `AuthService`) and call backend REST endpoints.

**PassengerComponent** — tabs: Search, Reservations, Check-ins, Messages, Reminders

| Tab | Endpoints |
|-----|-----------|
| Search | `GET /api/v1/search/results?q=&limit=10` |
| Reservations | `GET/POST /api/v1/passenger/reservations`, `PUT .../reservations/{id}` |
| Check-ins | `GET/POST /api/v1/passenger/checkins` |
| Messages | `GET/POST /api/v1/messages`, `POST /api/v1/messages/{id}/read` |
| Reminders | `GET/PUT /api/v1/passenger/reminder-preferences` |

**DispatcherComponent** — tabs: Workflows, Tasks

| Tab | Endpoints |
|-----|-----------|
| Workflows | `GET/POST /api/v1/workflows` |
| Tasks | `GET /api/v1/tasks`, `POST /api/v1/tasks/{id}/approve\|reject\|return` |

**AdminComponent** — tabs: Import, Ranking, Rules, Dictionaries, Users, Alerts, Diagnostics, Audit

| Tab | Endpoints |
|-----|-----------|
| Import | `POST /api/v1/admin/imports/run` |
| Ranking | `GET/PUT /api/v1/admin/ranking-config` |
| Rules | `GET/POST/DELETE /api/v1/admin/cleaning-rules` |
| Dictionaries | `GET/POST/DELETE /api/v1/admin/dictionaries` |
| Users | `GET /api/v1/admin/users`, `PUT /api/v1/admin/users/{id}` |
| Alerts | `GET/POST /api/v1/admin/alerts`, `POST .../alerts/{id}/acknowledge` |
| Diagnostics | `POST /api/v1/admin/diagnostics` |
| Audit | `GET /api/v1/admin/audit` |

### 4.6 Production vs development proxy

| Context | Proxy target | Configuration |
|---------|-------------|---------------|
| Production (Docker) | `http://backend:8080` | `frontend/nginx/default.conf` — nginx `proxy_pass` |
| Development (`ng serve`) | `http://localhost:8080` | `frontend/proxy.conf.json` — Angular dev-server proxy |

---

## 5 Service Layer Architecture

The backend follows a **layered architecture** within a modular monolith. Packages act as vertical feature slices, each containing its own controller, service, DTOs, and domain/repository references.

### 5.1 Package layout

```
com.eegalepoint.citybus
├── CityBusApplication              # @SpringBootApplication entry point
├── config/                          # Security, password, JWT, CORS beans
├── auth/                            # AuthController, AuthService, login DTOs
│   └── jwt/                         # JwtService, JwtAuthenticationFilter
├── audit/                           # LoginAuditService (JDBC)
├── rbac/                            # RoleDemoController
├── api/                             # ApiInfoController (/ping)
├── web/                             # ApiExceptionHandler (@RestControllerAdvice)
├── observability/                   # TraceIdFilter (X-Trace-Id)
├── ingestion/                       # CanonicalImportService, transactional helper, job lifecycle
├── transit/                         # RouteController, StopController, query services, DTOs
├── search/                          # SearchController, SearchService, DTOs
├── passenger/                       # PassengerController, PassengerService, DTOs
├── messaging/                       # MessageController, MessageService, DTOs
├── workflow/                        # WorkflowController, TaskController, WorkflowService, DTOs
├── admin/                           # AdminConfigController, AdminImportController,
│                                    #   OperationsController, services, DTOs
├── repo/                            # UserRepository
└── domain/                          # JPA entities and Spring Data repositories
    ├── config/                      #   Cleaning rules, audit logs, dictionaries
    ├── messaging/                   #   Messages, queue, attempts, redaction rules
    ├── operations/                  #   System alerts, diagnostic reports
    ├── passenger/                   #   Reservations, check-ins, reminders, DND
    ├── search/                      #   Ranking config, search events, popularity
    ├── transit/                     #   Routes, stops, versions, schedules, imports, mappings
    └── workflow/                    #   Definitions, instances, tasks, escalations
```

### 5.2 Layer responsibilities

| Layer | Stereotype | Responsibility |
|-------|-----------|----------------|
| **Controller** | `@RestController` | Request validation, HTTP mapping, `@PreAuthorize` enforcement, delegation to service |
| **Service** | `@Service` / `@Transactional` | Business logic, cross-entity coordination, transactional boundaries |
| **Repository** | `JpaRepository<E, Long>` | Data access via Spring Data JPA; custom `@Query` methods where needed |
| **Entity** | `@Entity` | JPA-mapped domain objects; no business logic |
| **DTO** | Plain records/classes | Request and response shapes; decoupled from entities |

### 5.3 Service inventory

| Service | Package | Key responsibilities |
|---------|---------|---------------------|
| **AuthService** | `auth` | Credential verification (BCrypt), JWT issuance, current-user resolution from `SecurityContext` |
| **JwtService** | `auth.jwt` | HS256 token creation and validation; claims include `sub` (username) and `roles` (list) |
| **LoginAuditService** | `audit` | JDBC insert of login attempts (user ID, IP, user-agent, success/failure) |
| **CanonicalImportService** | `ingestion` | Orchestrates canonical JSON import: validates payload, delegates to transactional helper, manages job lifecycle |
| **CanonicalImportTransactionalService** | `ingestion` | Single-transaction import of routes, stops, versions, route-stops, and schedules |
| **ImportJobLifecycleService** | `ingestion` | Creates, completes, and fails `SourceImportJobEntity` records |
| **RouteQueryService** | `transit` | Reads routes with their latest version, stops, and schedules |
| **StopQueryService** | `transit` | Reads a stop with its latest version |
| **SearchService** | `search` | Weighted text search across routes and stops; updates `stop_popularity_metrics` on result impressions; records `search_events` |
| **PassengerService** | `passenger` | Reservation CRUD, check-in creation, reminder preference management; auto-enqueues notification messages on reservation changes |
| **MessageService** | `messaging` | Message CRUD, read-marking, message creation with redaction rule application, message queue entry creation |
| **WorkflowService** | `workflow` | Workflow instance lifecycle, task CRUD, approve/reject/return decisions, batch decisions, auto-status rollup (all approved → COMPLETED; any rejected → REJECTED) |
| **AdminConfigService** | `admin` | Cleaning rule CRUD, dictionary CRUD, ranking config read/update, field mapping template listing, user listing/enable-disable, login audit log listing |
| **OperationsService** | `admin` | System alert CRUD with acknowledgment, diagnostic report execution (DB_HEALTH, TABLE_STATS, CONNECTION_POOL, FULL) |

### 5.4 Cross-cutting concerns

| Concern | Implementation |
|---------|---------------|
| **Request tracing** | `TraceIdFilter` reads or generates `X-Trace-Id`, sets it on the response header and in the SLF4J MDC for structured logging |
| **Global exception handling** | `ApiExceptionHandler` (`@RestControllerAdvice`) maps exceptions to consistent JSON error responses |
| **CORS** | `SecurityBeansConfig` provides a permissive `CorsConfigurationSource` (all origins, standard methods) |
| **Graceful shutdown** | `server.shutdown=graceful` with a 30-second lifecycle timeout |

---

## 6 Data Persistence Design

### 6.1 Strategy

- **Database:** PostgreSQL 16 (containerized via `postgres:16-alpine`)
- **Schema management:** Flyway with migrations under `classpath:db/migration` (V1–V9)
- **ORM:** Hibernate 6 via Spring Data JPA, `ddl-auto=validate` — Hibernate validates entity mappings against the Flyway-managed schema but never modifies it
- **Connection pool:** HikariCP with tuned settings (max pool 10, min idle 2, leak detection at 30 s, idle timeout 5 min, max lifetime 20 min)
- **Time zone:** All timestamps stored and read in UTC (`hibernate.jdbc.time_zone=UTC`)
- **Open-in-view:** Disabled (`spring.jpa.open-in-view=false`) to prevent lazy-loading outside transactional boundaries

### 6.2 Migration history

| Version | Name | Purpose |
|---------|------|---------|
| V1 | `auth_tables` | `roles`, `users`, `user_roles`, `password_history`, `login_audit` |
| V2 | `seed_roles_and_admin` | Seeds ADMIN, DISPATCHER, PASSENGER roles and the `admin` user |
| V3 | `seed_demo_users` | Seeds `dispatcher1` and `passenger1` with their roles |
| V4 | `transit_canonical` | `routes`, `route_versions`, `stops`, `stop_versions`, `route_stops`, `schedules`, `source_import_jobs`, `field_mappings` + DEFAULT_V1 mapping seed |
| V5 | `search_ranking` | `ranking_config`, `stop_popularity_metrics`, `search_events` + DEFAULT config seed |
| V6 | `passenger_messaging` | `passenger_reservations`, `passenger_checkins`, `reminder_preferences`, `do_not_disturb_windows`, `messages`, `message_queue`, `message_queue_attempts`, `message_redaction_rules` |
| V7 | `workflow` | `workflow_definitions`, `workflow_instances`, `workflow_tasks`, `workflow_escalations` + 3 definition seeds |
| V8 | `admin_config` | `cleaning_rule_sets`, `cleaning_audit_logs`, `field_standard_dictionaries` |
| V9 | `operations` | `system_alerts`, `diagnostic_reports` |

### 6.3 Schema overview (ERD summary)

```
┌───────────┐       ┌──────────────┐       ┌──────────────────┐
│   roles   │◀─M:M─▶│    users     │◀──1:N─│   login_audit    │
└───────────┘       └──────┬───────┘       └──────────────────┘
                           │
           ┌───────────────┼───────────────────────────┐
           │               │                           │
     ┌─────▼─────┐  ┌──────▼───────┐  ┌───────────────▼──────────┐
     │ messages   │  │ reservations │  │ reminder_preferences     │
     │            │  │              │  │ do_not_disturb_windows   │
     └─────┬─────┘  └──────┬───────┘  └──────────────────────────┘
           │               │
     ┌─────▼─────┐  ┌──────▼───────┐
     │  msg_queue │  │  checkins    │
     └─────┬─────┘  └──────────────┘
           │
     ┌─────▼──────────┐
     │ queue_attempts  │
     └────────────────┘

┌───────────┐     ┌────────────────┐     ┌─────────────┐
│  routes   │──1:N│ route_versions │──1:N│ route_stops  │
└───────────┘     └───────┬────────┘     └──────┬──────┘
                          │                     │
                    ┌─────▼──────┐        ┌─────▼────────┐
                    │ schedules  │        │ stop_versions │──N:1─▶ stops
                    └────────────┘        └──────────────┘

┌────────────────────┐     ┌────────────────────┐
│ workflow_definitions│──1:N│ workflow_instances  │
└────────────────────┘     └─────────┬──────────┘
                                     │
                               ┌─────▼──────────┐
                               │ workflow_tasks  │
                               └─────┬──────────┘
                                     │
                               ┌─────▼──────────────┐
                               │ workflow_escalations│
                               └─────────────────────┘

┌──────────────────┐     ┌─────────────────┐     ┌──────────────────────────┐
│ cleaning_rule_sets│──1:N│cleaning_audit_log│     │field_standard_dictionaries│
└──────────────────┘     └─────────────────┘     └──────────────────────────┘

┌───────────────┐     ┌────────────────────┐     ┌────────────────┐
│ ranking_config │     │stop_popularity_mtx │     │ search_events  │
└───────────────┘     └────────────────────┘     └────────────────┘

┌───────────────┐     ┌────────────────────┐
│ system_alerts │     │ diagnostic_reports │
└───────────────┘     └────────────────────┘
```

### 6.4 Repository layer

All repositories extend `JpaRepository<E, Long>`. Notable custom query methods:

| Repository | Custom methods |
|-----------|---------------|
| `UserRepository` | `findByUsername(String)` |
| `RouteVersionRepository` | `findMaxVersionNumber(routeId)`, `findFirstByRoute_IdOrderByVersionNumberDesc` |
| `RouteStopRepository` | `findByRouteVersionIdOrderBySequence` (fetch join) |
| `StopVersionRepository` | `findMaxVersionNumber(stopId)`, `findFirstByStop_IdOrderByVersionNumberDesc` |
| `ScheduleRepository` | `findByRouteVersionIdOrderByDepartureTimeAsc` |
| `SourceImportJobRepository` | `findAllByOrderByCreatedAtDesc` |
| `ReservationRepository` | `findByUser_IdOrderByReservedAtDesc` |
| `MessageRepository` | `findByUser_IdOrderByCreatedAtDesc`, `findByIdAndUser_Id` |
| `MessageRedactionRuleRepository` | `findByEnabledTrue` |
| `WorkflowInstanceRepository` | `findByStatusOrderByCreatedAtDesc`, `findAllByOrderByCreatedAtDesc` |
| `WorkflowTaskRepository` | `findByInstance_IdOrderByCreatedAtAsc`, `findByStatusOrderByCreatedAtDesc` |
| `CleaningRuleSetRepository` | `findByEnabledTrueOrderByNameAsc` |
| `SystemAlertRepository` | `findByAcknowledgedFalseOrderByCreatedAtDesc` |
| `RankingConfigRepository` | `findByConfigKey(String)` |

### 6.5 Internal message queue

Per ADR-001, the platform uses a **PostgreSQL-backed internal queue** instead of an external message broker.

```
messages ──1:1──▶ message_queue ──1:N──▶ message_queue_attempts
```

- `message_queue` tracks delivery status (`PENDING`, `SENT`, `FAILED`) and scheduling timestamps.
- `message_queue_attempts` records each delivery attempt with outcome and optional error message.
- `message_redaction_rules` apply regex-based content scrubbing before messages enter the queue.

This design keeps the deployment topology simple (no RabbitMQ/Kafka container) while providing auditable, retry-capable message delivery.

---

## 7 Model Overview

### 7.1 Authentication & identity

| Entity | Table | Key fields |
|--------|-------|------------|
| **UserEntity** | `users` | `id`, `username`, `passwordHash`, `enabled`, `createdAt`, `updatedAt`; `@ManyToMany` → `Set<RoleEntity>` via `user_roles` (EAGER fetch) |
| **RoleEntity** | `roles` | `id`, `name`, `description` |

### 7.2 Transit & ingestion

| Entity | Table | Key fields | Relations |
|--------|-------|------------|-----------|
| **RouteEntity** | `routes` | `id`, `code`, `createdAt` | — |
| **RouteVersionEntity** | `route_versions` | `id`, `versionNumber`, `name`, `effectiveFrom`, `createdAt` | `@ManyToOne` → `RouteEntity` |
| **StopEntity** | `stops` | `id`, `code`, `createdAt` | — |
| **StopVersionEntity** | `stop_versions` | `id`, `versionNumber`, `name`, `latitude`, `longitude`, `effectiveFrom`, `createdAt` | `@ManyToOne` → `StopEntity` |
| **RouteStopEntity** | `route_stops` | `id`, `stopSequence` | `@ManyToOne` → `RouteVersionEntity`, `StopVersionEntity` |
| **ScheduleEntity** | `schedules` | `id`, `tripCode`, `departureTime`, `createdAt` | `@ManyToOne` → `RouteVersionEntity` |
| **FieldMappingEntity** | `field_mappings` | `id`, `templateName`, `sourceField`, `targetField`, `createdAt` | — |
| **SourceImportJobEntity** | `source_import_jobs` | `id`, `sourceType`, `status`, `artifactName`, `rowCount`, `errorMessage`, timestamps | — |

The transit model uses a **versioning pattern**: `RouteEntity` and `StopEntity` are stable identifiers, while `RouteVersionEntity` and `StopVersionEntity` hold mutable attributes that change across data imports. `RouteStopEntity` links a specific route version to specific stop versions in sequence order.

### 7.3 Search & ranking

| Entity | Table | Key fields |
|--------|-------|------------|
| **RankingConfigEntity** | `ranking_config` | `id`, `configKey`, `routeWeight`, `stopWeight`, `popularityWeight`, `maxSuggestions`, `maxResults`, `updatedAt` |
| **StopPopularityMetricEntity** | `stop_popularity_metrics` | `stopId` (PK), `impressionCount`, `selectionCount`, `updatedAt` |
| **SearchEventEntity** | `search_events` | `id`, `queryText`, `scope`, `resultCount`, `createdAt` |

### 7.4 Passenger & messaging

| Entity | Table | Key fields | Relations |
|--------|-------|------------|-----------|
| **ReservationEntity** | `passenger_reservations` | `id`, `status`, `reservedAt`, `updatedAt` | `@ManyToOne` → `UserEntity`, `ScheduleEntity`, `StopEntity` |
| **CheckinEntity** | `passenger_checkins` | `id`, `checkedInAt` | `@ManyToOne` → `UserEntity`, `StopEntity`; optional `@ManyToOne` → `ReservationEntity` |
| **ReminderPreferenceEntity** | `reminder_preferences` | `id`, `enabled`, `minutesBefore`, `channel`, `updatedAt` | `@OneToOne` → `UserEntity` |
| **DoNotDisturbWindowEntity** | `do_not_disturb_windows` | `id`, `dayOfWeek`, `startTime`, `endTime`, `createdAt` | `@ManyToOne` → `UserEntity` |
| **MessageEntity** | `messages` | `id`, `subject`, `body`, `read`, `createdAt` | `@ManyToOne` → `UserEntity` |
| **MessageQueueEntity** | `message_queue` | `id`, `status`, `scheduledAt`, `sentAt`, `createdAt` | `@ManyToOne` → `MessageEntity` |
| **MessageQueueAttemptEntity** | `message_queue_attempts` | `id`, `attemptedAt`, `outcome`, `errorMessage` | `@ManyToOne` → `MessageQueueEntity` |
| **MessageRedactionRuleEntity** | `message_redaction_rules` | `id`, `pattern`, `replacement`, `enabled`, `createdAt` | — |

### 7.5 Workflow

| Entity | Table | Key fields | Relations |
|--------|-------|------------|-----------|
| **WorkflowDefinitionEntity** | `workflow_definitions` | `id`, `name`, `description`, `initialStatus`, `enabled`, `createdAt` | — |
| **WorkflowInstanceEntity** | `workflow_instances` | `id`, `title`, `status`, `createdAt`, `updatedAt` | `@ManyToOne` → `WorkflowDefinitionEntity`, `UserEntity` (createdBy), optional `UserEntity` (assignedTo) |
| **WorkflowTaskEntity** | `workflow_tasks` | `id`, `title`, `description`, `status`, `decisionNote`, `createdAt`, `updatedAt` | `@ManyToOne` → `WorkflowInstanceEntity`, optional `UserEntity` (assignedTo, decidedBy) |
| **WorkflowEscalationEntity** | `workflow_escalations` | `id`, `reason`, `createdAt` | `@ManyToOne` → `WorkflowTaskEntity`, `UserEntity` (escalatedTo) |

Workflow instances follow an automatic status rollup: when all tasks are approved the instance moves to **COMPLETED**; if any task is rejected the instance moves to **REJECTED**. A task that has already been decided returns HTTP 409 on a second decision attempt.

### 7.6 Admin configuration

| Entity | Table | Key fields | Relations |
|--------|-------|------------|-----------|
| **CleaningRuleSetEntity** | `cleaning_rule_sets` | `id`, `name`, `description`, `fieldTarget`, `ruleType`, `pattern`, `replacement`, `enabled`, timestamps | — |
| **CleaningAuditLogEntity** | `cleaning_audit_logs` | `id`, `originalValue`, `cleanedValue`, `createdAt` | `@ManyToOne` → `CleaningRuleSetEntity`, optional `UserEntity` |
| **FieldStandardDictionaryEntity** | `field_standard_dictionaries` | `id`, `fieldName`, `canonicalValue`, `aliases`, `enabled`, timestamps | — |

### 7.7 Operations

| Entity | Table | Key fields | Relations |
|--------|-------|------------|-----------|
| **SystemAlertEntity** | `system_alerts` | `id`, `severity`, `source`, `title`, `detail`, `acknowledged`, `acknowledgedAt`, `createdAt` | optional `@ManyToOne` → `UserEntity` (acknowledgedBy) |
| **DiagnosticReportEntity** | `diagnostic_reports` | `id`, `reportType`, `status`, `summary`, `detail`, `startedAt`, `completedAt` | optional `@ManyToOne` → `UserEntity` (triggeredBy) |

---

## 8 Authentication and Security Service

### 8.1 Architecture overview

```
  Client                         Backend
    │                               │
    │  POST /api/v1/auth/login      │
    │  { username, password }       │
    │──────────────────────────────▶│
    │                               ├─ Load UserEntity by username
    │                               ├─ Verify enabled flag
    │                               ├─ BCrypt password check
    │                               ├─ Build JWT (HS256)
    │                               ├─ Record login_audit
    │  { accessToken, roles }       │
    │◀──────────────────────────────│
    │                               │
    │  GET /api/v1/some-resource    │
    │  Authorization: Bearer <JWT>  │
    │──────────────────────────────▶│
    │                               ├─ JwtAuthenticationFilter
    │                               │   ├─ Extract Bearer token
    │                               │   ├─ JwtService.parseAndValidate()
    │                               │   └─ Set SecurityContext
    │                               ├─ @PreAuthorize check
    │                               ├─ Controller → Service → Repository
    │  { response }                 │
    │◀──────────────────────────────│
```

### 8.2 Components

| Component | Class | Responsibility |
|-----------|-------|----------------|
| **JWT creation** | `JwtService` | Creates HS256 tokens with `sub` = username, `roles` = list of role name strings; configurable secret (≥ 32 bytes) and expiration (default 3600 s) via `AppJwtProperties` |
| **JWT validation** | `JwtService` | Parses token, verifies HMAC signature, extracts claims |
| **Request filter** | `JwtAuthenticationFilter` | Runs before `UsernamePasswordAuthenticationFilter`; reads `Authorization: Bearer …`, validates via `JwtService`, sets `UsernamePasswordAuthenticationToken` with `ROLE_<name>` authorities |
| **Login** | `AuthService.login()` | Loads user by username, checks `enabled`, verifies BCrypt password, creates JWT, records audit entry |
| **Current user** | `AuthService.me()` | Reads `SecurityContext` authentication principal and returns user ID, username, and roles |
| **Audit** | `LoginAuditService` | JDBC insert into `login_audit` with user ID (nullable for failed attempts), username attempted, success flag, IP address, and user-agent |
| **Password encoding** | `PasswordConfig` | Provides `BCryptPasswordEncoder` bean |
| **Error responses** | `JsonAuthHandlers` | Returns `401 UNAUTHORIZED` JSON for unauthenticated requests and `403 FORBIDDEN` JSON for insufficient roles |

### 8.3 Security filter chain

Configured in `SecurityConfig` with `@EnableMethodSecurity`:

1. **CSRF:** Disabled (stateless API with JWT)
2. **Session management:** `STATELESS` — no `JSESSIONID`
3. **CORS:** Permissive defaults via `SecurityBeansConfig` (all origins, standard HTTP methods)
4. **Public endpoints:** `/actuator/**`, `/error`, `POST /api/v1/auth/login`, `OPTIONS /**`
5. **Protected endpoints:** All other paths require authentication
6. **Filter order:** `TraceIdFilter` → `JwtAuthenticationFilter` → Spring Security filters
7. **Exception handling:** Custom `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) from `JsonAuthHandlers`

### 8.4 Role-based access control

Method-level RBAC is enforced via `@PreAuthorize` annotations on controllers:

| Annotation | Applied to |
|------------|-----------|
| `hasRole('ADMIN')` | `AdminConfigController`, `AdminImportController`, `OperationsController` |
| `hasAnyRole('ADMIN','DISPATCHER')` | `WorkflowController`, `TaskController` |
| `hasAnyRole('ADMIN','PASSENGER')` | `PassengerController` |
| `isAuthenticated()` | `RouteController`, `StopController`, `SearchController`, `MessageController` |

JWT role claims are mapped to Spring Security authorities as `ROLE_<name>`, which aligns with Spring's `hasRole()` expression (which automatically prepends `ROLE_`).

### 8.5 Password policy

- Minimum 8 characters, enforced on the `LoginRequest` DTO via Bean Validation
- Passwords stored as BCrypt hashes
- `password_history` table exists for future password-reuse prevention

### 8.6 Seed credentials

Provisioned by Flyway migrations V2 and V3 (idempotent `ON CONFLICT DO NOTHING`):

| Username | Password | Role |
|----------|----------|------|
| `admin` | `ChangeMe123!` | ADMIN |
| `dispatcher1` | `ChangeMe123!` | DISPATCHER |
| `passenger1` | `ChangeMe123!` | PASSENGER |

### 8.7 Observability integration

- **`X-Trace-Id`:** The `TraceIdFilter` runs before authentication. It reads an incoming `X-Trace-Id` header or generates a UUID, sets it on the response, and places it in the SLF4J MDC as `traceId` for inclusion in every log line.
- **Login audit:** Every login attempt (success or failure) is recorded with timestamp, IP address, user-agent, and the user ID when available.
- **Structured logging:** Log pattern includes `[%X{traceId:-}]` for correlation across request lifecycle.
