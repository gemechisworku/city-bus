package com.eegalepoint.citybus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class WorkflowIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("citybus")
          .withUsername("citybus")
          .withPassword("citybus");

  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired TestRestTemplate restTemplate;

  private Long routeChangeDefId;

  @BeforeEach
  void cleanup() {
    jdbcTemplate.update("DELETE FROM workflow_escalations");
    jdbcTemplate.update("DELETE FROM workflow_tasks");
    jdbcTemplate.update("DELETE FROM workflow_instances");

    routeChangeDefId = jdbcTemplate.queryForObject(
        "SELECT id FROM workflow_definitions WHERE name = 'ROUTE_CHANGE'", Long.class);
  }

  @Test
  void workflowsRequireAuth() {
    ResponseEntity<String> res = restTemplate.getForEntity("/api/v1/workflows", String.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void passengerCannotAccessWorkflows() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);
    ResponseEntity<String> res = restTemplate.exchange(
        "/api/v1/workflows", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void dispatcherCanCreateWorkflowAndTasks() {
    String token = loginToken("dispatcher1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> wfBody = Map.of("definitionId", routeChangeDefId, "title", "Update Route 42");
    ResponseEntity<Map> wf = restTemplate.exchange(
        "/api/v1/workflows", HttpMethod.POST, new HttpEntity<>(wfBody, headers), Map.class);
    assertThat(wf.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(wf.getBody()).containsEntry("status", "OPEN");
    assertThat(wf.getBody()).containsEntry("definitionName", "ROUTE_CHANGE");
    long instanceId = ((Number) wf.getBody().get("id")).longValue();

    Map<String, Object> taskBody = Map.of(
        "instanceId", instanceId, "title", "Review stops", "description", "Check stop locations");
    ResponseEntity<Map> task = restTemplate.exchange(
        "/api/v1/tasks", HttpMethod.POST, new HttpEntity<>(taskBody, headers), Map.class);
    assertThat(task.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(task.getBody()).containsEntry("status", "PENDING");
    assertThat(task.getBody()).containsEntry("title", "Review stops");
  }

  @Test
  void approveRejectReturnFlow() {
    String token = loginToken("dispatcher1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    long instanceId = createWorkflow(headers);

    long task1 = createTask(headers, instanceId, "Task A");
    long task2 = createTask(headers, instanceId, "Task B");
    long task3 = createTask(headers, instanceId, "Task C");

    ResponseEntity<Map> returned = restTemplate.exchange(
        "/api/v1/tasks/" + task3 + "/return", HttpMethod.POST,
        new HttpEntity<>(Map.of("note", "Need more info"), headers), Map.class);
    assertThat(returned.getBody()).containsEntry("status", "RETURNED");

    ResponseEntity<Map> approved = restTemplate.exchange(
        "/api/v1/tasks/" + task1 + "/approve", HttpMethod.POST,
        new HttpEntity<>(Map.of("note", "Looks good"), headers), Map.class);
    assertThat(approved.getBody()).containsEntry("status", "APPROVED");

    ResponseEntity<Map> instance = restTemplate.exchange(
        "/api/v1/workflows/" + instanceId, HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);
    assertThat(instance.getBody()).containsEntry("status", "OPEN");

    restTemplate.exchange(
        "/api/v1/tasks/" + task2 + "/approve", HttpMethod.POST,
        new HttpEntity<>(headers), Map.class);
    restTemplate.exchange(
        "/api/v1/tasks/" + task3 + "/approve", HttpMethod.POST,
        new HttpEntity<>(Map.of("note", "Info received"), headers), Map.class);

    ResponseEntity<Map> completed = restTemplate.exchange(
        "/api/v1/workflows/" + instanceId, HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);
    assertThat(completed.getBody()).containsEntry("status", "COMPLETED");
  }

  @Test
  void rejectTaskSetsWorkflowRejected() {
    String token = loginToken("dispatcher1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    long instanceId = createWorkflow(headers);
    long taskId = createTask(headers, instanceId, "Review");

    restTemplate.exchange(
        "/api/v1/tasks/" + taskId + "/reject", HttpMethod.POST,
        new HttpEntity<>(Map.of("note", "Not feasible"), headers), Map.class);

    ResponseEntity<Map> instance = restTemplate.exchange(
        "/api/v1/workflows/" + instanceId, HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);
    assertThat(instance.getBody()).containsEntry("status", "REJECTED");
  }

  @Test
  void doubleDecisionReturnsConflict() {
    String token = loginToken("dispatcher1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    long instanceId = createWorkflow(headers);
    long taskId = createTask(headers, instanceId, "Decide once");

    restTemplate.exchange(
        "/api/v1/tasks/" + taskId + "/approve", HttpMethod.POST,
        new HttpEntity<>(headers), Map.class);

    ResponseEntity<String> again = restTemplate.exchange(
        "/api/v1/tasks/" + taskId + "/reject", HttpMethod.POST,
        new HttpEntity<>(headers), String.class);
    assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void batchApprove() {
    String token = loginToken("dispatcher1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    long instanceId = createWorkflow(headers);
    long t1 = createTask(headers, instanceId, "Batch A");
    long t2 = createTask(headers, instanceId, "Batch B");

    Map<String, Object> batch = Map.of(
        "taskIds", List.of(t1, t2), "action", "APPROVE", "note", "All good");
    ResponseEntity<Map> res = restTemplate.exchange(
        "/api/v1/tasks/batch", HttpMethod.POST,
        new HttpEntity<>(batch, headers), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Number) res.getBody().get("processed")).intValue()).isEqualTo(2);
  }

  @Test
  void listTasksByStatus() {
    String token = loginToken("dispatcher1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    long instanceId = createWorkflow(headers);
    createTask(headers, instanceId, "Pending task");

    ResponseEntity<List> pending = restTemplate.exchange(
        "/api/v1/tasks?status=PENDING", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(pending.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(pending.getBody()).isNotEmpty();
  }

  @Test
  void listWorkflowsByStatus() {
    String token = loginToken("dispatcher1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    createWorkflow(headers);

    ResponseEntity<List> open = restTemplate.exchange(
        "/api/v1/workflows?status=OPEN", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(open.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(open.getBody()).isNotEmpty();
  }

  private long createWorkflow(HttpHeaders headers) {
    Map<String, Object> body = Map.of("definitionId", routeChangeDefId, "title", "Test WF");
    ResponseEntity<Map> res = restTemplate.exchange(
        "/api/v1/workflows", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    return ((Number) res.getBody().get("id")).longValue();
  }

  private long createTask(HttpHeaders headers, long instanceId, String title) {
    Map<String, Object> body = Map.of("instanceId", instanceId, "title", title);
    ResponseEntity<Map> res = restTemplate.exchange(
        "/api/v1/tasks", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    return ((Number) res.getBody().get("id")).longValue();
  }

  private String loginToken(String user, String pass) {
    ResponseEntity<Map> login = restTemplate.postForEntity(
        "/api/v1/auth/login", Map.of("username", user, "password", pass), Map.class);
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (String) login.getBody().get("accessToken");
  }

  private HttpHeaders bearerHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
