# City Bus — REST API Specification

**Base URL:** `/api/v1`
**Content type:** `application/json; charset=UTF-8`
**Authentication:** Bearer JWT via `Authorization: Bearer <token>` header (unless marked Public)
**Tracing:** Clients may send `X-Trace-Id` on any request; the server echoes or generates one on every response

---

## Table of Contents

1. [Conventions](#1-conventions)
2. [Error Responses](#2-error-responses)
3. [Authentication](#3-authentication)
4. [RBAC Demo](#4-rbac-demo)
5. [Transit — Routes](#5-transit--routes)
6. [Transit — Stops](#6-transit--stops)
7. [Transit — Import](#7-transit--import)
8. [Search](#8-search)
9. [Passenger — Reservations](#9-passenger--reservations)
10. [Passenger — Check-ins](#10-passenger--check-ins)
11. [Passenger — Reminder Preferences](#11-passenger--reminder-preferences)
12. [Messages](#12-messages)
13. [Workflows](#13-workflows)
14. [Tasks](#14-tasks)
15. [Admin — Ranking Config](#15-admin--ranking-config)
16. [Admin — Field Mapping Templates](#16-admin--field-mapping-templates)
17. [Admin — Cleaning Rules](#17-admin--cleaning-rules)
18. [Admin — Dictionaries](#18-admin--dictionaries)
19. [Admin — Users](#19-admin--users)
20. [Admin — Audit Log](#20-admin--audit-log)
21. [Operations — Alerts](#21-operations--alerts)
22. [Operations — Diagnostics](#22-operations--diagnostics)
23. [Utility](#23-utility)
24. [Actuator](#24-actuator)

---

## 1 Conventions

| Item | Rule |
|------|------|
| Path IDs | `Long` — numeric, e.g. `/routes/42` |
| Timestamps | ISO-8601 UTC — `2026-04-13T14:30:00.000Z` (Java `Instant`) |
| Dates | ISO-8601 — `2026-04-13` (Java `LocalDate`) |
| Times | ISO-8601 local — `08:30:00` (Java `LocalTime`) |
| Decimals | JSON number — `1.50` (Java `BigDecimal`) |
| Booleans | JSON `true` / `false` |
| Nullability | Fields are non-null unless marked *nullable* |
| Pagination | Not implemented; lists return all matching rows |

### Authorization shorthand

| Tag | Meaning |
|-----|---------|
| **Public** | No token required |
| **Authenticated** | Any valid JWT |
| **ADMIN** | `hasRole('ADMIN')` |
| **DISPATCHER** | `hasAnyRole('ADMIN', 'DISPATCHER')` |
| **PASSENGER** | `hasAnyRole('ADMIN', 'PASSENGER')` |

---

## 2 Error Responses

All errors return a JSON object. The HTTP status code indicates the error category.

### 400 — Validation Error

```json
{
  "error": "VALIDATION",
  "message": "username: must not be blank; password: size must be between 8 and 128"
}
```

### 401 — Unauthorized

```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid credentials"
}
```

### 403 — Forbidden

```json
{
  "error": "FORBIDDEN",
  "message": "Access Denied"
}
```

### 404 — Not Found

Returned when a resource identified by a path ID does not exist.

### 409 — Conflict

Returned when a business rule prevents the action (e.g. deciding an already-decided task).

---

## 3 Authentication

### 3.1 Login

`POST /api/v1/auth/login` — **Public**

Authenticates a user and returns a JWT.

**Request body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `username` | string | yes | Not blank, max 128 chars |
| `password` | string | yes | Not blank, 8–128 chars |

```json
{
  "username": "admin",
  "password": "ChangeMe123!"
}
```

**Response:** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | string | JWT (HS256) |
| `tokenType` | string | Always `"Bearer"` |
| `expiresInSeconds` | long | Token TTL (default 3600) |

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600
}
```

**Error responses:** `400` validation, `401` invalid credentials

---

### 3.2 Logout

`POST /api/v1/auth/logout` — **Authenticated**

Stateless logout (idempotent, for client-side token disposal).

**Request body:** none

**Response:** `204 No Content`

---

### 3.3 Current User

`GET /api/v1/auth/me` — **Authenticated**

Returns the identity and roles of the authenticated user.

**Response:** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `userId` | long | User primary key |
| `username` | string | Login username |
| `roles` | string[] | Role names (e.g. `["ADMIN"]`) |

```json
{
  "userId": 1,
  "username": "admin",
  "roles": ["ADMIN"]
}
```

---

## 4 RBAC Demo

Diagnostic endpoints to verify role enforcement. Useful for integration testing.

### 4.1 Admin-only

`GET /api/v1/demo/admin` — **ADMIN**

**Response:** `200 OK`

```json
{ "scope": "admin" }
```

### 4.2 Dispatcher-access

`GET /api/v1/demo/dispatcher` — **DISPATCHER**

**Response:** `200 OK`

```json
{ "scope": "dispatcher" }
```

### 4.3 Passenger-access

`GET /api/v1/demo/passenger` — **PASSENGER**

**Response:** `200 OK`

```json
{ "scope": "passenger" }
```

---

## 5 Transit — Routes

### 5.1 List Routes

`GET /api/v1/routes` — **Authenticated**

Returns all routes with their latest version metadata.

**Response:** `200 OK` — `RouteSummaryResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Route ID |
| `code` | string | Route code (unique) |
| `name` | string | Route name (from latest version) |
| `latestVersionNumber` | int | Latest version number |
| `latestRouteVersionId` | long | Latest route version ID |

```json
[
  {
    "id": 1,
    "code": "R001",
    "name": "Downtown Express",
    "latestVersionNumber": 2,
    "latestRouteVersionId": 5
  }
]
```

---

### 5.2 Get Route Detail

`GET /api/v1/routes/{id}` — **Authenticated**

Returns a route with its latest version, stops (in sequence order), and schedules.

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Route ID |

**Response:** `200 OK` — `RouteDetailResponse`

| Field | Type | Description |
|-------|------|-------------|
| `routeId` | long | Route ID |
| `code` | string | Route code |
| `versionNumber` | int | Version number |
| `name` | string | Route name |
| `effectiveFrom` | date | *nullable* — effective date |
| `stops` | StopOnRouteResponse[] | Ordered stops |
| `schedules` | ScheduleResponse[] | Departure schedules |

**StopOnRouteResponse:**

| Field | Type | Description |
|-------|------|-------------|
| `sequence` | int | Stop order on route |
| `stopCode` | string | Stop code |
| `name` | string | Stop name |
| `latitude` | decimal | *nullable* |
| `longitude` | decimal | *nullable* |
| `effectiveFrom` | date | *nullable* |

**ScheduleResponse:**

| Field | Type | Description |
|-------|------|-------------|
| `tripCode` | string | *nullable* — trip identifier |
| `departureTime` | time | Departure time (HH:mm:ss) |

```json
{
  "routeId": 1,
  "code": "R001",
  "versionNumber": 2,
  "name": "Downtown Express",
  "effectiveFrom": "2026-04-01",
  "stops": [
    {
      "sequence": 1,
      "stopCode": "S001",
      "name": "Central Station",
      "latitude": 40.7128,
      "longitude": -74.0060,
      "effectiveFrom": "2026-04-01"
    }
  ],
  "schedules": [
    { "tripCode": "T-AM1", "departureTime": "07:30:00" }
  ]
}
```

**Error responses:** `404` route not found

---

## 6 Transit — Stops

### 6.1 Get Stop Detail

`GET /api/v1/stops/{id}` — **Authenticated**

Returns a stop with its latest version.

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Stop ID |

**Response:** `200 OK` — `StopDetailResponse`

| Field | Type | Description |
|-------|------|-------------|
| `stopId` | long | Stop ID |
| `code` | string | Stop code |
| `versionNumber` | int | Version number |
| `name` | string | Stop name |
| `latitude` | decimal | *nullable* |
| `longitude` | decimal | *nullable* |
| `effectiveFrom` | date | *nullable* |

```json
{
  "stopId": 1,
  "code": "S001",
  "versionNumber": 1,
  "name": "Central Station",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "effectiveFrom": "2026-04-01"
}
```

**Error responses:** `404` stop not found

---

## 7 Transit — Import

### 7.1 Run Import

`POST /api/v1/admin/imports/run` — **ADMIN**

Imports canonical transit data (routes, stops, schedules) from a JSON payload.

**Request body:** — `CanonicalImportRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `templateName` | string | yes | Not blank |
| `routes` | RouteImportDto[] | yes | Not empty |

**RouteImportDto:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `routeCode` | string | yes | Not blank |
| `name` | string | yes | Not blank |
| `effectiveFrom` | date | no | |
| `stops` | StopImportDto[] | yes | Not empty |
| `schedules` | ScheduleImportDto[] | no | Defaults to `[]` |

**StopImportDto:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `stopCode` | string | yes | Not blank |
| `name` | string | yes | Not blank |
| `latitude` | decimal | no | |
| `longitude` | decimal | no | |
| `sequence` | int | yes | Min 1 |
| `effectiveFrom` | date | no | |

**ScheduleImportDto:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `tripCode` | string | no | |
| `departureTime` | time | yes | Not null |

```json
{
  "templateName": "DEFAULT_V1",
  "routes": [
    {
      "routeCode": "R001",
      "name": "Downtown Express",
      "effectiveFrom": "2026-04-01",
      "stops": [
        { "stopCode": "S001", "name": "Central Station", "latitude": 40.7128, "longitude": -74.006, "sequence": 1 },
        { "stopCode": "S002", "name": "Market Square", "latitude": 40.7150, "longitude": -74.008, "sequence": 2 }
      ],
      "schedules": [
        { "tripCode": "T-AM1", "departureTime": "07:30:00" },
        { "tripCode": "T-PM1", "departureTime": "17:30:00" }
      ]
    }
  ]
}
```

**Response:** `200 OK` — `ImportRunResponse`

| Field | Type | Description |
|-------|------|-------------|
| `jobId` | long | Import job ID |
| `status` | string | `COMPLETED` or `FAILED` |
| `rowCount` | int | *nullable* — rows imported |
| `errorMessage` | string | *nullable* — error details |

```json
{
  "jobId": 1,
  "status": "COMPLETED",
  "rowCount": 4,
  "errorMessage": null
}
```

---

### 7.2 List Import Jobs

`GET /api/v1/admin/imports` — **ADMIN**

Returns all import jobs ordered by creation date descending.

**Response:** `200 OK` — `ImportJobSummaryResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Job ID |
| `sourceType` | string | Import template used |
| `status` | string | `PENDING`, `COMPLETED`, `FAILED` |
| `artifactName` | string | *nullable* — artifact identifier |
| `rowCount` | int | *nullable* — rows processed |
| `errorMessage` | string | *nullable* — error details |
| `createdAt` | timestamp | Job creation time |
| `startedAt` | timestamp | *nullable* — start time |
| `completedAt` | timestamp | *nullable* — completion time |

---

## 8 Search

### 8.1 Search Suggestions

`GET /api/v1/search/suggestions` — **Authenticated**

Returns type-ahead suggestions for routes and stops.

**Query parameters:**

| Param | Type | Required | Validation |
|-------|------|----------|------------|
| `q` | string | yes | 2–128 chars; letters, digits, spaces, hyphens only |
| `limit` | int | no | Max results (uses ranking config `maxSuggestions` default) |

**Response:** `200 OK` — `SearchHitDto[]`

| Field | Type | Description |
|-------|------|-------------|
| `kind` | string | `ROUTE` or `STOP` |
| `id` | long | Entity ID |
| `code` | string | Route or stop code |
| `name` | string | Name |
| `score` | double | Relevance score |

```json
[
  { "kind": "ROUTE", "id": 1, "code": "R001", "name": "Downtown Express", "score": 8.5 },
  { "kind": "STOP", "id": 3, "code": "S003", "name": "Downtown Plaza", "score": 6.2 }
]
```

**Error responses:** `400` invalid query

---

### 8.2 Search Results

`GET /api/v1/search/results` — **Authenticated**

Returns full ranked search results. Updates `stop_popularity_metrics` impressions for stops in the result set.

**Query parameters:**

| Param | Type | Required | Validation |
|-------|------|----------|------------|
| `q` | string | yes | 2–128 chars; letters, digits, spaces, hyphens only |
| `limit` | int | no | Max results (uses ranking config `maxResults` default) |

**Response:** `200 OK` — `SearchHitDto[]`

Same schema as suggestions (see [8.1](#81-search-suggestions)).

---

## 9 Passenger — Reservations

### 9.1 Create Reservation

`POST /api/v1/passenger/reservations` — **PASSENGER**

Creates a reservation for a schedule at a stop. Automatically enqueues a notification message.

**Request body:** — `CreateReservationRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `scheduleId` | long | yes | Not null |
| `stopId` | long | yes | Not null |

```json
{
  "scheduleId": 1,
  "stopId": 3
}
```

**Response:** `201 Created` — `ReservationResponse`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Reservation ID |
| `scheduleId` | long | Schedule ID |
| `tripCode` | string | Trip code from schedule |
| `departureTime` | time | Departure time |
| `stopId` | long | Stop ID |
| `stopCode` | string | Stop code |
| `stopName` | string | Stop name |
| `status` | string | `PENDING` |
| `reservedAt` | timestamp | Creation timestamp |
| `updatedAt` | timestamp | Last update timestamp |

```json
{
  "id": 1,
  "scheduleId": 1,
  "tripCode": "T-AM1",
  "departureTime": "07:30:00",
  "stopId": 3,
  "stopCode": "S003",
  "stopName": "Downtown Plaza",
  "status": "PENDING",
  "reservedAt": "2026-04-13T10:00:00Z",
  "updatedAt": "2026-04-13T10:00:00Z"
}
```

---

### 9.2 List Reservations

`GET /api/v1/passenger/reservations` — **PASSENGER**

Returns the current user's reservations ordered by reserved-at descending.

**Response:** `200 OK` — `ReservationResponse[]`

Same schema as [9.1](#91-create-reservation) response.

---

### 9.3 Update Reservation

`PUT /api/v1/passenger/reservations/{id}` — **PASSENGER**

Updates the status of a reservation. Automatically enqueues a notification message.

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Reservation ID |

**Request body:** — `UpdateReservationRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `status` | string | yes | Not blank; must be `CONFIRMED` or `CANCELLED` |

```json
{
  "status": "CONFIRMED"
}
```

**Response:** `200 OK` — `ReservationResponse`

**Error responses:** `400` invalid status, `404` not found

---

## 10 Passenger — Check-ins

### 10.1 Create Check-in

`POST /api/v1/passenger/checkins` — **PASSENGER**

Records a check-in at a stop, optionally linked to a reservation.

**Request body:** — `CreateCheckinRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `stopId` | long | yes | Not null |
| `reservationId` | long | no | *nullable* — links to a reservation |

```json
{
  "stopId": 3,
  "reservationId": 1
}
```

**Response:** `201 Created` — `CheckinResponse`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Check-in ID |
| `stopId` | long | Stop ID |
| `stopCode` | string | Stop code |
| `stopName` | string | Stop name |
| `reservationId` | long | *nullable* — linked reservation |
| `checkedInAt` | timestamp | Check-in timestamp |

---

### 10.2 List Check-ins

`GET /api/v1/passenger/checkins` — **PASSENGER**

Returns the current user's check-ins ordered by checked-in-at descending.

**Response:** `200 OK` — `CheckinResponse[]`

---

## 11 Passenger — Reminder Preferences

### 11.1 Get Reminder Preferences

`GET /api/v1/passenger/reminder-preferences` — **PASSENGER**

Returns the current user's reminder preferences (defaults if none saved).

**Response:** `200 OK` — `ReminderPreferenceResponse`

| Field | Type | Description |
|-------|------|-------------|
| `enabled` | boolean | Reminders enabled |
| `minutesBefore` | int | Minutes before departure |
| `channel` | string | `IN_APP`, `EMAIL`, or `SMS` |

```json
{
  "enabled": true,
  "minutesBefore": 15,
  "channel": "IN_APP"
}
```

---

### 11.2 Update Reminder Preferences

`PUT /api/v1/passenger/reminder-preferences` — **PASSENGER**

Creates or updates reminder preferences for the current user.

**Request body:** — `UpdateReminderPreferenceRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `enabled` | boolean | yes | Not null |
| `minutesBefore` | int | yes | Not null, 1–1440 |
| `channel` | string | yes | Not blank; must be `IN_APP`, `EMAIL`, or `SMS` |

```json
{
  "enabled": true,
  "minutesBefore": 30,
  "channel": "EMAIL"
}
```

**Response:** `200 OK` — `ReminderPreferenceResponse`

---

## 12 Messages

### 12.1 List Messages

`GET /api/v1/messages` — **Authenticated**

Returns the current user's messages ordered by created-at descending.

**Response:** `200 OK` — `MessageResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Message ID |
| `subject` | string | Subject line |
| `body` | string | Message body (redacted) |
| `read` | boolean | Read status |
| `createdAt` | timestamp | Creation timestamp |

---

### 12.2 Get Message

`GET /api/v1/messages/{id}` — **Authenticated**

Returns a single message belonging to the current user.

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Message ID |

**Response:** `200 OK` — `MessageResponse`

**Error responses:** `404` not found (or not owned by user)

---

### 12.3 Mark Message as Read

`POST /api/v1/messages/{id}/read` — **Authenticated**

Marks a message as read.

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Message ID |

**Request body:** none

**Response:** `200 OK` — `MessageResponse` (with `read: true`)

---

### 12.4 Create Message

`POST /api/v1/messages` — **Authenticated**

Creates a new message for the current user. The body is processed through active redaction rules before storage. A message queue entry is created for delivery.

**Request body:** — `CreateMessageRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `subject` | string | yes | Not blank, max 255 chars |
| `body` | string | yes | Not blank |

```json
{
  "subject": "Reservation Confirmed",
  "body": "Your reservation for trip T-AM1 has been confirmed."
}
```

**Response:** `201 Created` — `MessageResponse`

---

## 13 Workflows

### 13.1 Create Workflow Instance

`POST /api/v1/workflows` — **DISPATCHER**

Creates a new workflow instance from a workflow definition.

**Request body:** — `CreateWorkflowRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `definitionId` | long | yes | Not null |
| `title` | string | yes | Not blank, max 255 chars |

```json
{
  "definitionId": 1,
  "title": "Route R001 schedule update"
}
```

**Response:** `201 Created` — `WorkflowInstanceResponse`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Instance ID |
| `definitionName` | string | Workflow definition name |
| `title` | string | Instance title |
| `status` | string | `OPEN` |
| `createdByUsername` | string | Creator's username |
| `assignedToUsername` | string | *nullable* — assigned user |
| `tasks` | TaskResponse[] | Attached tasks (empty on creation) |
| `createdAt` | timestamp | Creation time |
| `updatedAt` | timestamp | Last update time |

**Seeded workflow definitions:**

| ID | Name |
|----|------|
| 1 | `ROUTE_CHANGE` |
| 2 | `SCHEDULE_CHANGE` |
| 3 | `PASSENGER_COMPLAINT` |

---

### 13.2 List Workflow Instances

`GET /api/v1/workflows` — **DISPATCHER**

Returns workflow instances with embedded tasks.

**Query parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | string | no | Filter by status (e.g. `OPEN`, `COMPLETED`, `REJECTED`) |

**Response:** `200 OK` — `WorkflowInstanceResponse[]`

---

### 13.3 Get Workflow Instance

`GET /api/v1/workflows/{instanceId}` — **DISPATCHER**

Returns a single workflow instance with its tasks.

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `instanceId` | long | Instance ID |

**Response:** `200 OK` — `WorkflowInstanceResponse`

**Error responses:** `404` not found

---

## 14 Tasks

### 14.1 Create Task

`POST /api/v1/tasks` — **DISPATCHER**

Adds a task to an existing workflow instance.

**Request body:** — `CreateTaskRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `instanceId` | long | yes | Not null |
| `title` | string | yes | Not blank, max 255 chars |
| `description` | string | no | *nullable* |
| `assignedToUserId` | long | no | *nullable* — user to assign |

```json
{
  "instanceId": 1,
  "title": "Review schedule changes",
  "description": "Verify departure times are correct",
  "assignedToUserId": 2
}
```

**Response:** `201 Created` — `TaskResponse`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Task ID |
| `instanceId` | long | Parent instance ID |
| `title` | string | Task title |
| `description` | string | *nullable* |
| `status` | string | `PENDING` |
| `assignedToUsername` | string | *nullable* — assigned user |
| `decidedByUsername` | string | *nullable* — decision maker |
| `decisionNote` | string | *nullable* — decision note |
| `createdAt` | timestamp | Creation time |
| `updatedAt` | timestamp | Last update time |

---

### 14.2 List Tasks

`GET /api/v1/tasks` — **DISPATCHER**

Returns tasks, optionally filtered by status.

**Query parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | string | no | Filter by status (e.g. `PENDING`, `APPROVED`, `REJECTED`, `RETURNED`) |

**Response:** `200 OK` — `TaskResponse[]`

---

### 14.3 Approve Task

`POST /api/v1/tasks/{id}/approve` — **DISPATCHER**

Approves a pending task. If all tasks on the parent instance are now approved, the instance auto-transitions to `COMPLETED`.

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Task ID |

**Request body:** — `TaskDecisionRequest` *(optional)*

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `note` | string | no | Decision note |

```json
{ "note": "Looks good, approved." }
```

**Response:** `200 OK` — `TaskResponse`

**Error responses:** `404` not found, `409` already decided

---

### 14.4 Reject Task

`POST /api/v1/tasks/{id}/reject` — **DISPATCHER**

Rejects a pending task. The parent instance auto-transitions to `REJECTED`.

**Path parameters, request body, response:** same as [14.3](#143-approve-task)

---

### 14.5 Return Task

`POST /api/v1/tasks/{id}/return` — **DISPATCHER**

Returns a task for further work (resets it to `RETURNED` status; does not trigger instance rollup).

**Path parameters, request body, response:** same as [14.3](#143-approve-task)

---

### 14.6 Batch Task Decision

`POST /api/v1/tasks/batch` — **DISPATCHER**

Applies a single action to multiple tasks at once.

**Request body:** — `BatchTaskDecisionRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `taskIds` | long[] | yes | Not empty |
| `action` | string | yes | Not blank; must be `APPROVE`, `REJECT`, or `RETURN` |
| `note` | string | no | *nullable* — decision note |

```json
{
  "taskIds": [1, 2, 3],
  "action": "APPROVE",
  "note": "Batch approval for sprint 5"
}
```

**Response:** `200 OK` — `BatchResultResponse`

| Field | Type | Description |
|-------|------|-------------|
| `processed` | int | Number of tasks processed |
| `tasks` | TaskResponse[] | Updated task states |

```json
{
  "processed": 3,
  "tasks": [
    { "id": 1, "instanceId": 1, "title": "...", "status": "APPROVED", "..." : "..." },
    { "id": 2, "instanceId": 1, "title": "...", "status": "APPROVED", "..." : "..." },
    { "id": 3, "instanceId": 2, "title": "...", "status": "APPROVED", "..." : "..." }
  ]
}
```

---

## 15 Admin — Ranking Config

### 15.1 Get Ranking Config

`GET /api/v1/admin/ranking-config` — **ADMIN**

Returns the current search ranking weights and limits.

**Response:** `200 OK` — `RankingConfigResponse`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Config row ID |
| `configKey` | string | Config key (e.g. `DEFAULT`) |
| `routeWeight` | decimal | Weight for route name matches |
| `stopWeight` | decimal | Weight for stop name matches |
| `popularityWeight` | decimal | Weight for stop popularity |
| `maxSuggestions` | int | Max suggestion results |
| `maxResults` | int | Max search results |
| `updatedAt` | timestamp | Last update time |

```json
{
  "id": 1,
  "configKey": "DEFAULT",
  "routeWeight": 1.0,
  "stopWeight": 1.0,
  "popularityWeight": 0.5,
  "maxSuggestions": 10,
  "maxResults": 20,
  "updatedAt": "2026-04-13T00:00:00Z"
}
```

---

### 15.2 Update Ranking Config

`PUT /api/v1/admin/ranking-config` — **ADMIN**

Updates search ranking weights and limits.

**Request body:** — `UpdateRankingConfigRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `routeWeight` | decimal | yes | Not null, >= 0.0 |
| `stopWeight` | decimal | yes | Not null, >= 0.0 |
| `popularityWeight` | decimal | yes | Not null, >= 0.0 |
| `maxSuggestions` | int | yes | Not null, >= 1 |
| `maxResults` | int | yes | Not null, >= 1 |

```json
{
  "routeWeight": 1.5,
  "stopWeight": 1.0,
  "popularityWeight": 0.8,
  "maxSuggestions": 15,
  "maxResults": 25
}
```

**Response:** `200 OK` — `RankingConfigResponse`

---

## 16 Admin — Field Mapping Templates

### 16.1 List Templates

`GET /api/v1/admin/templates` — **ADMIN**

Returns all field mapping templates used by the import pipeline.

**Response:** `200 OK` — `FieldMappingResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Mapping ID |
| `templateName` | string | Template name (e.g. `DEFAULT_V1`) |
| `sourceField` | string | Source JSON field path |
| `targetField` | string | Target canonical field |
| `createdAt` | timestamp | Creation time |

```json
[
  {
    "id": 1,
    "templateName": "DEFAULT_V1",
    "sourceField": "routeCode",
    "targetField": "code",
    "createdAt": "2026-04-13T00:00:00Z"
  }
]
```

---

## 17 Admin — Cleaning Rules

### 17.1 List Cleaning Rules

`GET /api/v1/admin/cleaning-rules` — **ADMIN**

Returns all data cleaning rules.

**Response:** `200 OK` — `CleaningRuleResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Rule ID |
| `name` | string | Rule name (unique) |
| `description` | string | *nullable* |
| `fieldTarget` | string | Target field for the rule |
| `ruleType` | string | Rule type |
| `pattern` | string | Regex pattern |
| `replacement` | string | *nullable* — replacement text |
| `enabled` | boolean | Whether rule is active |
| `createdAt` | timestamp | Creation time |
| `updatedAt` | timestamp | Last update time |

---

### 17.2 Create Cleaning Rule

`POST /api/v1/admin/cleaning-rules` — **ADMIN**

**Request body:** — `SaveCleaningRuleRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | string | yes | Not blank, max 128 chars |
| `description` | string | no | |
| `fieldTarget` | string | yes | Not blank |
| `pattern` | string | yes | Not blank, max 512 chars |
| `replacement` | string | no | |
| `enabled` | boolean | yes | Not null |

```json
{
  "name": "Strip trailing whitespace",
  "description": "Remove trailing spaces from stop names",
  "fieldTarget": "stop_name",
  "pattern": "\\s+$",
  "replacement": "",
  "enabled": true
}
```

**Response:** `201 Created` — `CleaningRuleResponse`

---

### 17.3 Update Cleaning Rule

`PUT /api/v1/admin/cleaning-rules/{id}` — **ADMIN**

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Rule ID |

**Request body:** same as [17.2](#172-create-cleaning-rule)

**Response:** `200 OK` — `CleaningRuleResponse`

**Error responses:** `404` not found

---

### 17.4 Delete Cleaning Rule

`DELETE /api/v1/admin/cleaning-rules/{id}` — **ADMIN**

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Rule ID |

**Response:** `204 No Content`

---

## 18 Admin — Dictionaries

### 18.1 List Dictionary Entries

`GET /api/v1/admin/dictionaries` — **ADMIN**

Returns all field standardization dictionary entries.

**Response:** `200 OK` — `DictionaryEntryResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Entry ID |
| `fieldName` | string | Target field name |
| `canonicalValue` | string | Standardized value |
| `aliases` | string | *nullable* — comma-separated aliases |
| `enabled` | boolean | Whether entry is active |
| `createdAt` | timestamp | Creation time |
| `updatedAt` | timestamp | Last update time |

---

### 18.2 Create Dictionary Entry

`POST /api/v1/admin/dictionaries` — **ADMIN**

**Request body:** — `SaveDictionaryEntryRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `fieldName` | string | yes | Not blank, max 128 chars |
| `canonicalValue` | string | yes | Not blank, max 255 chars |
| `aliases` | string | no | |
| `enabled` | boolean | yes | Not null |

```json
{
  "fieldName": "city",
  "canonicalValue": "New York",
  "aliases": "NYC, New York City, NY",
  "enabled": true
}
```

**Response:** `201 Created` — `DictionaryEntryResponse`

---

### 18.3 Update Dictionary Entry

`PUT /api/v1/admin/dictionaries/{id}` — **ADMIN**

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Entry ID |

**Request body:** same as [18.2](#182-create-dictionary-entry)

**Response:** `200 OK` — `DictionaryEntryResponse`

**Error responses:** `404` not found

---

### 18.4 Delete Dictionary Entry

`DELETE /api/v1/admin/dictionaries/{id}` — **ADMIN**

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Entry ID |

**Response:** `204 No Content`

---

## 19 Admin — Users

### 19.1 List Users

`GET /api/v1/admin/users` — **ADMIN**

Returns all registered users with their roles.

**Response:** `200 OK` — `UserAdminResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | User ID |
| `username` | string | Username |
| `enabled` | boolean | Account enabled |
| `roles` | string[] | Role names |
| `createdAt` | timestamp | Registration time |

```json
[
  { "id": 1, "username": "admin", "enabled": true, "roles": ["ADMIN"], "createdAt": "2026-04-13T00:00:00Z" },
  { "id": 2, "username": "dispatcher1", "enabled": true, "roles": ["DISPATCHER"], "createdAt": "2026-04-13T00:00:00Z" }
]
```

---

### 19.2 Get User

`GET /api/v1/admin/users/{id}` — **ADMIN**

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | User ID |

**Response:** `200 OK` — `UserAdminResponse`

**Error responses:** `404` not found

---

### 19.3 Update User

`PUT /api/v1/admin/users/{id}` — **ADMIN**

Enable or disable a user account.

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | User ID |

**Request body:** — `UpdateUserRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `enabled` | boolean | yes | Not null |

```json
{ "enabled": false }
```

**Response:** `200 OK` — `UserAdminResponse`

---

## 20 Admin — Audit Log

### 20.1 List Login Audit Entries

`GET /api/v1/admin/audit` — **ADMIN**

Returns the most recent login audit entries (up to 200).

**Response:** `200 OK` — `AuditLogResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Audit entry ID |
| `userId` | long | *nullable* — user ID (null for failed lookups) |
| `usernameAttempt` | string | Username used in login attempt |
| `success` | boolean | Login outcome |
| `ipAddress` | string | Client IP address |
| `createdAt` | timestamp | Attempt timestamp |

```json
[
  {
    "id": 42,
    "userId": 1,
    "usernameAttempt": "admin",
    "success": true,
    "ipAddress": "172.18.0.1",
    "createdAt": "2026-04-13T14:22:00Z"
  }
]
```

---

## 21 Operations — Alerts

### 21.1 List Alerts

`GET /api/v1/admin/alerts` — **ADMIN**

Returns system alerts ordered by created-at descending.

**Query parameters:**

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `unacknowledged` | boolean | no | `false` | If `true`, returns only unacknowledged alerts |

**Response:** `200 OK` — `SystemAlertResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Alert ID |
| `severity` | string | `INFO`, `WARN`, `ERROR`, `CRITICAL` |
| `source` | string | Alert source identifier |
| `title` | string | Alert title |
| `detail` | string | *nullable* — additional detail |
| `acknowledged` | boolean | Acknowledgment status |
| `acknowledgedByUsername` | string | *nullable* — user who acknowledged |
| `acknowledgedAt` | timestamp | *nullable* — acknowledgment time |
| `createdAt` | timestamp | Creation time |

---

### 21.2 Create Alert

`POST /api/v1/admin/alerts` — **ADMIN**

Creates a system alert.

**Request body:** — `CreateAlertRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `severity` | string | yes | Not blank; must be `INFO`, `WARN`, `ERROR`, or `CRITICAL` |
| `source` | string | yes | Not blank, max 128 chars |
| `title` | string | yes | Not blank, max 255 chars |
| `detail` | string | no | *nullable* |

```json
{
  "severity": "WARN",
  "source": "import-pipeline",
  "title": "Import took longer than expected",
  "detail": "Route R005 import completed in 45s (threshold: 30s)"
}
```

**Response:** `201 Created` — `SystemAlertResponse`

---

### 21.3 Acknowledge Alert

`POST /api/v1/admin/alerts/{id}/acknowledge` — **ADMIN**

Marks an alert as acknowledged by the current user.

**Path parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | long | Alert ID |

**Request body:** none

**Response:** `200 OK` — `SystemAlertResponse` (with `acknowledged: true`)

**Error responses:** `404` not found

---

## 22 Operations — Diagnostics

### 22.1 List Diagnostic Reports

`GET /api/v1/admin/diagnostics` — **ADMIN**

Returns historical diagnostic reports ordered by started-at descending.

**Response:** `200 OK` — `DiagnosticReportResponse[]`

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Report ID |
| `reportType` | string | `DB_HEALTH`, `TABLE_STATS`, `CONNECTION_POOL`, or `FULL` |
| `status` | string | Report status |
| `summary` | string | Human-readable summary |
| `detail` | string | *nullable* — raw diagnostic data |
| `triggeredByUsername` | string | *nullable* — user who triggered |
| `startedAt` | timestamp | Start time |
| `completedAt` | timestamp | *nullable* — completion time |

---

### 22.2 Run Diagnostic

`POST /api/v1/admin/diagnostics` — **ADMIN**

Runs a diagnostic report and returns the result.

**Request body:** — `RunDiagnosticRequest`

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `reportType` | string | yes | Not blank; must be `DB_HEALTH`, `TABLE_STATS`, `CONNECTION_POOL`, or `FULL` |

```json
{ "reportType": "FULL" }
```

**Response:** `201 Created` — `DiagnosticReportResponse`

---

## 23 Utility

### 23.1 Ping

`GET /api/v1/ping` — **Authenticated**

Health-check endpoint for the API layer.

**Response:** `200 OK`

```json
{
  "status": "ok",
  "service": "city-bus-api"
}
```

---

## 24 Actuator

Spring Boot Actuator endpoints are exposed outside the `/api/v1` namespace. Health detail components (`db`, `diskspace`) are visible only to authorized users.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/actuator/health` | GET | Application health (UP/DOWN); shows db/diskspace components for authorized users |
| `/actuator/info` | GET | Application info |
| `/actuator/metrics` | GET | Application metrics |
| `/actuator/flyway` | GET | Flyway migration status |
| `/actuator/env` | GET | Environment properties |
