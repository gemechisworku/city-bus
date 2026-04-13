# City Bus Platform — Issue Verification & Criteria Re-Audit (`audit-report-check-fix4`)

**Date:** 2026-04-13  
**Audit type:** Static-only (no Maven/npm/Docker execution in this pass)  
**Baselines:** [`.tmp/audit-report2.md`](./audit-report2.md)   
**Repository evaluated:** `d:\EegalePointAI\city-bus\repo`

---

## Executive summary

| Category | Assessment |
|----------|------------|
| **Issues #1–#13 (`audit-report2`) — static closure** | **13 / 13** addressed in code for the originally cited gaps; **#8** is met for **join-style conditional gating** (multi-predecessor tasks) rather than a full **BPMN/OR-split** engine. |
| **Residual items R-1–R-4 (`fix2`)** | **R-1, R-2, R-3: Closed.** **R-4: Closed** for *join / “all predecessors approved”* semantics via **`workflow_task_dependencies`** + API + UI; **not** a general arbitrary DAG with exclusive gateways unless interpreted narrowly. |
| **§6 “Admin/debug demo” (`fix2`)** | **Closed** — `RoleDemoController` is **`@ConditionalOnProperty`**: default **`app.demo-endpoints-enabled=false`** (`application.yml`); **`true`** in **`application-test.yml`** for IT that hits `/api/v1/demo/*`. |
| **§7–§8.3 “unit tests” (`fix2`)** | **Improved** — non-Spring **`PinyinServiceTest`**, Mockito **`MessageQueueProcessorTest`**; integration tests remain primary. |
| **Documentation (`api-outline`)** | **Aligned** — Phase 6 (`predecessorTaskId` / `predecessorTaskIds`), Phase 0–1 (Actuator security), demo routes, notification templates (Phase 7). |
| **Overall verdict vs session criteria** | **Pass** — remaining gaps are **product-depth / BPMN** (optional) and **runtime** confirmation (tests not executed here). |

---

## 1. Verdict

**Overall conclusion: Pass**

**Reasoning:** The tree under `repo/` shows closure of the historical audit gaps: notification templates, dictionary `aliases` shape, Micrometer HTTP P95, queue backlog monitoring, multi-predecessor workflow dependencies (**Flyway `V13`**, **`WorkflowTaskDependencyEntity`**, **`WorkflowService`** join enforcement, **`WorkflowIT.dualPredecessorsBlockUntilBothApproved`**), dispatcher **create-task** UX with **comma-separated predecessor IDs**, **`docs/api-outline.md`** corrections, and **demo RBAC endpoints** disabled by default. Backend **unit-level** tests now exist alongside **`*IT.java`**. What remains “out of scope” for a typical delivery audit is a **full workflow definition language** (exclusive/inclusive gateways, subprocesses) unless the prompt is read as requiring that level of product.

---

## 2. Issue-by-issue snapshot (`audit-report2.md` #1–#13)

| # | Topic | Status (fix4 static view) | Evidence (indicative) |
|---|--------|---------------------------|-------------------------|
| 1 | Actuator exposure | **Yes** | `SecurityConfig` health/info public; `/actuator/**` ADMIN; `application.yml` exposure list; ITs in `ObservabilityIT` / `CrossUserIsolationIT`. |
| 2 | Message queue consumer | **Yes** | `MessageQueueProcessor`; **`MessageQueueProcessorTest`** (mocked). |
| 3 | DND API | **Yes** | Passenger + `CrossUserIsolationIT` (unchanged finding). |
| 4 | Pinyin / initials | **Yes** | `SearchService` / `PinyinService`; **`PinyinServiceTest`**. |
| 5 | Workflow escalation | **Yes** | `WorkflowEscalationScheduler` (unchanged). |
| 6 | Cleaning on import | **Yes** | `DataCleaningService` on import path (unchanged). |
| 7 | HTML import | **Yes** | `HtmlImportService` / admin import (unchanged). |
| 8 | Conditional branching | **Yes (join model)** | **`V13__workflow_task_dependencies.sql`**; **`predecessorTaskIds`** + legacy **`predecessorTaskId`**; **`TaskResponse.predecessorTaskIds`**; **`WorkflowIT`** predecessor + dual-predecessor tests; dispatcher UI. **Not** arbitrary graph/OR-splits unless specified elsewhere. |
| 9 | Reminder default | **Yes** | Service + Flyway V10 (unchanged). |
| 10 | Dictionary `aliases` | **Yes** | Admin posts **string** (`admin.component.ts`). |
| 11 | Notification templates | **Yes** | V11 + CRUD + admin tab + `AdminConsoleIT` (unchanged). |
| 12 | P95 / backlog | **Yes** | `MetricsMonitorScheduler` + Micrometer; backlog JDBC (unchanged). |
| 13 | JWT defaults | **Yes** | `application.yml` secret from env; test profile default (unchanged). |

---

## 3. Residual items R-1–R-4 — closure (fix4)

| ID | Original concern | fix4 status |
|----|-------------------|-------------|
| **R-1** | Notification templates | **Closed** |
| **R-2** | Dictionary `aliases` | **Closed** |
| **R-3** | HTTP P95 metrics | **Closed** |
| **R-4** | Workflow branching / DAG | **Closed** for **multi-predecessor join**; **optional** follow-up for exclusive OR / full process model. |

---

## 4. Security review (vs `fix2` §6)

| Dimension | fix4 |
|-----------|------|
| Authentication / RBAC | **Pass** (unchanged pattern). |
| Actuator | **Pass** — docs + config consistent. |
| **`/api/v1/demo/*`** | **Pass** — **off** unless `app.demo-endpoints-enabled=true`; tests enable explicitly. |

---

## 5. Tests and coverage

| Metric | fix4 (static grep / file list) |
|--------|--------------------------------|
| Integration `*IT.java` | **8** classes; **`WorkflowIT`** includes **12** `@Test` (includes dual-predecessor case). |
| Unit `*Test.java` (no IT suffix) | **2** classes — **`PinyinServiceTest`** (2 methods), **`MessageQueueProcessorTest`** (1 method). |
| Approx. total `@Test` in `repo/backend/src/test` | **~74** (includes duplicate path indexing on case-insensitive trees — treat as approximate). |

**Not executed in this pass:** `./repo/run_test.sh`, Maven, npm, Testcontainers.

---

## 6. Documentation

| Artifact | fix4 |
|----------|------|
| **`repo/docs/api-outline.md`** | Documents **`predecessorTaskIds`**, join semantics, Actuator security, **`env` not exposed**, **`app.demo-endpoints-enabled`**, notification templates vs import templates. |

---

## 7. Residual / optional follow-ups (low severity)

| Topic | Note |
|-------|------|
| **Exclusive OR / BPMN** | Not implemented; only **AND-join** over predecessors. |
| **Scheduler side effects in IT** | Escalation / metrics schedulers still not exhaustively asserted via time-travel IT (acceptable for many teams). |
| **Frontend E2E** | Still not present; Karma specs only. |

---

## 8. Comparison to `audit-report-check-fix3.md`

**fix3** listed **documentation drift** for `api-outline` and **dispatcher** lacking predecessor UX — both are **resolved** in the current tree. **fix3**’s **R-4 “partial”** for sequential-only predecessors is **superseded** by **multi-predecessor join** + **V13**. Use **this file (`fix4`)** for the latest static closure picture.

---

## 9. Scope of this evaluation

### Reviewed (representative)
- `repo/backend` — workflow, security, observability, admin, Flyway `V11`–`V13`, tests  
- `repo/frontend` — admin, dispatcher (task create / predecessors)  
- `repo/docs/api-outline.md`

### Not executed
- Builds, tests, containers, browser.

---

## 10. Final note

For a **production** sign-off, still run **`./repo/run_test.sh`** (or Maven + npm CI) in an environment with **JDK 17+** and **Node**, and perform spot browser QA on dispatcher workflow creation.

---

*End of report.*
