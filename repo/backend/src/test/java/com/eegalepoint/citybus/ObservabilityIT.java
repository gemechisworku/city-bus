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
class ObservabilityIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("citybus")
          .withUsername("citybus")
          .withPassword("citybus");

  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired TestRestTemplate restTemplate;

  @BeforeEach
  void cleanup() {
    jdbcTemplate.update("DELETE FROM diagnostic_reports");
    jdbcTemplate.update("DELETE FROM system_alerts");
  }

  @Test
  void healthEndpointPubliclyAccessible() {
    ResponseEntity<Map> res = restTemplate.getForEntity("/actuator/health", Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(res.getBody()).containsEntry("status", "UP");
  }

  @Test
  void metricsEndpointRequiresAdmin() {
    ResponseEntity<Map> unauth = restTemplate.getForEntity("/actuator/metrics", Map.class);
    assertThat(unauth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    String token = loginToken("admin", "ChangeMe123!");
    ResponseEntity<Map> res = restTemplate.exchange(
        "/actuator/metrics", HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(token)), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void traceIdHeaderReturned() {
    ResponseEntity<String> res = restTemplate.getForEntity("/actuator/health", String.class);
    assertThat(res.getHeaders().getFirst("X-Trace-Id")).isNotNull();
  }

  @Test
  void alertsRequireAdmin() {
    assertThat(restTemplate.getForEntity("/api/v1/admin/alerts", String.class)
        .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    String dispToken = loginToken("dispatcher1", "ChangeMe123!");
    assertThat(restTemplate.exchange("/api/v1/admin/alerts", HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(dispToken)), String.class)
        .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void alertCreateListAcknowledge() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of(
        "severity", "WARN",
        "source", "import-service",
        "title", "Import latency above threshold",
        "detail", "Average import time: 12s (threshold: 5s)");
    ResponseEntity<Map> created = restTemplate.exchange(
        "/api/v1/admin/alerts", HttpMethod.POST,
        new HttpEntity<>(body, headers), Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).containsEntry("severity", "WARN");
    assertThat(created.getBody()).containsEntry("acknowledged", false);
    long alertId = ((Number) created.getBody().get("id")).longValue();

    ResponseEntity<List> list = restTemplate.exchange(
        "/api/v1/admin/alerts", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(list.getBody()).hasSize(1);

    ResponseEntity<List> unack = restTemplate.exchange(
        "/api/v1/admin/alerts?unacknowledged=true", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(unack.getBody()).hasSize(1);

    ResponseEntity<Map> ack = restTemplate.exchange(
        "/api/v1/admin/alerts/" + alertId + "/acknowledge", HttpMethod.POST,
        new HttpEntity<>(headers), Map.class);
    assertThat(ack.getBody()).containsEntry("acknowledged", true);
    assertThat(ack.getBody().get("acknowledgedByUsername")).isEqualTo("admin");

    ResponseEntity<List> afterAck = restTemplate.exchange(
        "/api/v1/admin/alerts?unacknowledged=true", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(afterAck.getBody()).isEmpty();
  }

  @Test
  void doubleAcknowledgeReturnsConflict() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of(
        "severity", "INFO", "source", "test", "title", "Test alert");
    ResponseEntity<Map> created = restTemplate.exchange(
        "/api/v1/admin/alerts", HttpMethod.POST,
        new HttpEntity<>(body, headers), Map.class);
    long alertId = ((Number) created.getBody().get("id")).longValue();

    restTemplate.exchange(
        "/api/v1/admin/alerts/" + alertId + "/acknowledge", HttpMethod.POST,
        new HttpEntity<>(headers), Map.class);

    ResponseEntity<String> again = restTemplate.exchange(
        "/api/v1/admin/alerts/" + alertId + "/acknowledge", HttpMethod.POST,
        new HttpEntity<>(headers), String.class);
    assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void diagnosticDbHealth() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of("reportType", "DB_HEALTH");
    ResponseEntity<Map> res = restTemplate.exchange(
        "/api/v1/admin/diagnostics", HttpMethod.POST,
        new HttpEntity<>(body, headers), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(res.getBody()).containsEntry("status", "COMPLETED");
    assertThat(res.getBody()).containsEntry("reportType", "DB_HEALTH");
    assertThat((String) res.getBody().get("summary")).contains("Database reachable");
  }

  @Test
  void diagnosticTableStats() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of("reportType", "TABLE_STATS");
    ResponseEntity<Map> res = restTemplate.exchange(
        "/api/v1/admin/diagnostics", HttpMethod.POST,
        new HttpEntity<>(body, headers), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(res.getBody()).containsEntry("status", "COMPLETED");
    assertThat((String) res.getBody().get("detail")).contains("rows");
  }

  @Test
  void diagnosticFull() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of("reportType", "FULL");
    ResponseEntity<Map> res = restTemplate.exchange(
        "/api/v1/admin/diagnostics", HttpMethod.POST,
        new HttpEntity<>(body, headers), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(res.getBody()).containsEntry("status", "COMPLETED");
    assertThat((String) res.getBody().get("detail")).contains("DB_HEALTH");
  }

  @Test
  void diagnosticsListPersists() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    restTemplate.exchange(
        "/api/v1/admin/diagnostics", HttpMethod.POST,
        new HttpEntity<>(Map.of("reportType", "DB_HEALTH"), headers), Map.class);

    ResponseEntity<List> list = restTemplate.exchange(
        "/api/v1/admin/diagnostics", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(list.getBody()).isNotEmpty();
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
