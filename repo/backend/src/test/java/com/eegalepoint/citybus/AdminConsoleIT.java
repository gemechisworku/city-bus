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
class AdminConsoleIT {

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
    jdbcTemplate.update("DELETE FROM cleaning_audit_logs");
    jdbcTemplate.update("DELETE FROM cleaning_rule_sets");
    jdbcTemplate.update("DELETE FROM field_standard_dictionaries");
  }

  @Test
  void adminEndpointsRequireAuth() {
    assertThat(restTemplate.getForEntity("/api/v1/admin/cleaning-rules", String.class)
        .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(restTemplate.getForEntity("/api/v1/admin/dictionaries", String.class)
        .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(restTemplate.getForEntity("/api/v1/admin/ranking-config", String.class)
        .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void nonAdminCannotAccessAdminEndpoints() {
    String token = loginToken("dispatcher1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    assertThat(restTemplate.exchange("/api/v1/admin/cleaning-rules", HttpMethod.GET,
        new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    assertThat(restTemplate.exchange("/api/v1/admin/users", HttpMethod.GET,
        new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void cleaningRuleCrud() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of(
        "name", "trim-spaces",
        "fieldTarget", "stop_name",
        "pattern", "\\s+",
        "replacement", " ",
        "enabled", true);

    ResponseEntity<Map> created = restTemplate.exchange(
        "/api/v1/admin/cleaning-rules", HttpMethod.POST,
        new HttpEntity<>(body, headers), Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).containsEntry("name", "trim-spaces");
    long ruleId = ((Number) created.getBody().get("id")).longValue();

    ResponseEntity<List> list = restTemplate.exchange(
        "/api/v1/admin/cleaning-rules", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(list.getBody()).hasSize(1);

    Map<String, Object> update = Map.of(
        "name", "trim-spaces-v2",
        "fieldTarget", "stop_name",
        "pattern", "\\s{2,}",
        "replacement", " ",
        "enabled", true);
    ResponseEntity<Map> updated = restTemplate.exchange(
        "/api/v1/admin/cleaning-rules/" + ruleId, HttpMethod.PUT,
        new HttpEntity<>(update, headers), Map.class);
    assertThat(updated.getBody()).containsEntry("name", "trim-spaces-v2");

    ResponseEntity<Void> deleted = restTemplate.exchange(
        "/api/v1/admin/cleaning-rules/" + ruleId, HttpMethod.DELETE,
        new HttpEntity<>(headers), Void.class);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void dictionaryCrud() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of(
        "fieldName", "route_type",
        "canonicalValue", "EXPRESS",
        "aliases", "exp,express,fast",
        "enabled", true);

    ResponseEntity<Map> created = restTemplate.exchange(
        "/api/v1/admin/dictionaries", HttpMethod.POST,
        new HttpEntity<>(body, headers), Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).containsEntry("canonicalValue", "EXPRESS");
    long dictId = ((Number) created.getBody().get("id")).longValue();

    ResponseEntity<List> list = restTemplate.exchange(
        "/api/v1/admin/dictionaries", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(list.getBody()).isNotEmpty();

    Map<String, Object> update = Map.of(
        "fieldName", "route_type",
        "canonicalValue", "EXPRESS",
        "aliases", "exp,express,fast,rapid",
        "enabled", true);
    ResponseEntity<Map> updated = restTemplate.exchange(
        "/api/v1/admin/dictionaries/" + dictId, HttpMethod.PUT,
        new HttpEntity<>(update, headers), Map.class);
    assertThat(((String) updated.getBody().get("aliases"))).contains("rapid");

    ResponseEntity<Void> deleted = restTemplate.exchange(
        "/api/v1/admin/dictionaries/" + dictId, HttpMethod.DELETE,
        new HttpEntity<>(headers), Void.class);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void rankingConfigGetAndUpdate() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    ResponseEntity<Map> get = restTemplate.exchange(
        "/api/v1/admin/ranking-config", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);
    assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(get.getBody()).containsEntry("configKey", "DEFAULT");

    Map<String, Object> body = Map.of(
        "routeWeight", 2.0,
        "stopWeight", 1.5,
        "popularityWeight", 0.8,
        "maxSuggestions", 10,
        "maxResults", 25);
    ResponseEntity<Map> updated = restTemplate.exchange(
        "/api/v1/admin/ranking-config", HttpMethod.PUT,
        new HttpEntity<>(body, headers), Map.class);
    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Number) updated.getBody().get("maxResults")).intValue()).isEqualTo(25);
  }

  @Test
  void templatesListReturnsFieldMappings() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    ResponseEntity<List> list = restTemplate.exchange(
        "/api/v1/admin/templates", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody()).isNotEmpty();
  }

  @Test
  void usersListAndUpdate() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    ResponseEntity<List> list = restTemplate.exchange(
        "/api/v1/admin/users", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().size()).isGreaterThanOrEqualTo(3);

    Long passengerId = jdbcTemplate.queryForObject(
        "SELECT id FROM users WHERE username = 'passenger1'", Long.class);

    ResponseEntity<Map> detail = restTemplate.exchange(
        "/api/v1/admin/users/" + passengerId, HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);
    assertThat(detail.getBody()).containsEntry("username", "passenger1");

    Map<String, Object> disable = Map.of("enabled", false);
    ResponseEntity<Map> updated = restTemplate.exchange(
        "/api/v1/admin/users/" + passengerId, HttpMethod.PUT,
        new HttpEntity<>(disable, headers), Map.class);
    assertThat(updated.getBody()).containsEntry("enabled", false);

    Map<String, Object> enable = Map.of("enabled", true);
    restTemplate.exchange(
        "/api/v1/admin/users/" + passengerId, HttpMethod.PUT,
        new HttpEntity<>(enable, headers), Map.class);
  }

  @Test
  void auditLogReturnsLoginHistory() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    ResponseEntity<List> list = restTemplate.exchange(
        "/api/v1/admin/audit", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
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
