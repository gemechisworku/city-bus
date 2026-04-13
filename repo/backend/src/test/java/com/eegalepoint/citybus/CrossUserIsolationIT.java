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
class CrossUserIsolationIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("citybus")
          .withUsername("citybus")
          .withPassword("citybus");

  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired TestRestTemplate restTemplate;

  private Long scheduleId;
  private Long stopId;

  @BeforeEach
  void seed() {
    jdbcTemplate.update("DELETE FROM message_queue_attempts");
    jdbcTemplate.update("DELETE FROM message_queue");
    jdbcTemplate.update("DELETE FROM messages");
    jdbcTemplate.update("DELETE FROM passenger_checkins");
    jdbcTemplate.update("DELETE FROM passenger_reservations");
    jdbcTemplate.update("DELETE FROM do_not_disturb_windows");
    jdbcTemplate.update("DELETE FROM route_stops");
    jdbcTemplate.update("DELETE FROM schedules");
    jdbcTemplate.update("DELETE FROM route_versions");
    jdbcTemplate.update("DELETE FROM routes");
    jdbcTemplate.update("DELETE FROM stop_versions");
    jdbcTemplate.update("DELETE FROM stops");

    jdbcTemplate.update("INSERT INTO routes (code) VALUES ('ISO-R1')");
    Long routeId = jdbcTemplate.queryForObject("SELECT id FROM routes WHERE code = 'ISO-R1'", Long.class);
    jdbcTemplate.update("INSERT INTO route_versions (route_id, version_number, name) VALUES (?, 1, 'Iso Route')", routeId);
    Long rvId = jdbcTemplate.queryForObject("SELECT id FROM route_versions WHERE route_id = ? AND version_number = 1", Long.class, routeId);

    jdbcTemplate.update("INSERT INTO stops (code) VALUES ('ISO-S1')");
    stopId = jdbcTemplate.queryForObject("SELECT id FROM stops WHERE code = 'ISO-S1'", Long.class);
    jdbcTemplate.update("INSERT INTO stop_versions (stop_id, version_number, name, latitude, longitude) VALUES (?, 1, 'Iso Stop', 40.0, -74.0)", stopId);

    jdbcTemplate.update("INSERT INTO schedules (route_version_id, trip_code, departure_time) VALUES (?, 'ISO-T1', '09:00')", rvId);
    scheduleId = jdbcTemplate.queryForObject("SELECT id FROM schedules WHERE trip_code = 'ISO-T1'", Long.class);
  }

  @Test
  void userACannotReadUserBMessages() {
    String tokenA = loginToken("passenger1", "ChangeMe123!");
    String tokenB = loginToken("dispatcher1", "ChangeMe123!");

    Map<String, Object> msgBody = Map.of("subject", "Private A", "body", "Secret A content");
    ResponseEntity<Map> created = restTemplate.exchange(
        "/api/v1/messages", HttpMethod.POST,
        new HttpEntity<>(msgBody, bearerHeaders(tokenA)), Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    long msgId = ((Number) created.getBody().get("id")).longValue();

    ResponseEntity<Map> attempt = restTemplate.exchange(
        "/api/v1/messages/" + msgId, HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(tokenB)), Map.class);
    assertThat(attempt.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void userACannotSeeUserBReservations() {
    String tokenA = loginToken("passenger1", "ChangeMe123!");

    Map<String, Object> body = Map.of("scheduleId", scheduleId, "stopId", stopId);
    ResponseEntity<Map> create = restTemplate.exchange(
        "/api/v1/passenger/reservations", HttpMethod.POST,
        new HttpEntity<>(body, bearerHeaders(tokenA)), Map.class);
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    long resId = ((Number) create.getBody().get("id")).longValue();

    String tokenAdmin = loginToken("admin", "ChangeMe123!");
    ResponseEntity<List> adminList = restTemplate.exchange(
        "/api/v1/passenger/reservations", HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(tokenAdmin)), List.class);
    assertThat(adminList.getStatusCode()).isEqualTo(HttpStatus.OK);
    boolean found = adminList.getBody().stream()
        .anyMatch(item -> {
          Map<?, ?> map = (Map<?, ?>) item;
          return ((Number) map.get("id")).longValue() == resId;
        });
    assertThat(found).as("Admin should not see passenger1's reservation in their own list").isFalse();
  }

  @Test
  void userACannotCancelUserBReservation() {
    String tokenA = loginToken("passenger1", "ChangeMe123!");

    Map<String, Object> body = Map.of("scheduleId", scheduleId, "stopId", stopId);
    ResponseEntity<Map> create = restTemplate.exchange(
        "/api/v1/passenger/reservations", HttpMethod.POST,
        new HttpEntity<>(body, bearerHeaders(tokenA)), Map.class);
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    long resId = ((Number) create.getBody().get("id")).longValue();

    String tokenAdmin = loginToken("admin", "ChangeMe123!");
    Map<String, Object> cancel = Map.of("status", "CANCELLED");
    ResponseEntity<String> attempt = restTemplate.exchange(
        "/api/v1/passenger/reservations/" + resId, HttpMethod.PUT,
        new HttpEntity<>(cancel, bearerHeaders(tokenAdmin)), String.class);
    assertThat(attempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void dndWindowCrud() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of("dayOfWeek", 0, "startTime", "22:00:00", "endTime", "07:00:00");
    ResponseEntity<Map> created = restTemplate.exchange(
        "/api/v1/passenger/dnd-windows", HttpMethod.POST,
        new HttpEntity<>(body, headers), Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).containsEntry("dayOfWeek", 0);
    long dndId = ((Number) created.getBody().get("id")).longValue();

    ResponseEntity<List> list = restTemplate.exchange(
        "/api/v1/passenger/dnd-windows", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody()).hasSize(1);

    ResponseEntity<String> del = restTemplate.exchange(
        "/api/v1/passenger/dnd-windows/" + dndId, HttpMethod.DELETE,
        new HttpEntity<>(headers), String.class);
    assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<List> emptyList = restTemplate.exchange(
        "/api/v1/passenger/dnd-windows", HttpMethod.GET,
        new HttpEntity<>(headers), List.class);
    assertThat(emptyList.getBody()).isEmpty();
  }

  @Test
  void userACannotDeleteUserBDndWindow() {
    String tokenA = loginToken("passenger1", "ChangeMe123!");
    String tokenAdmin = loginToken("admin", "ChangeMe123!");

    Map<String, Object> body = Map.of("dayOfWeek", 1, "startTime", "23:00:00", "endTime", "06:00:00");
    ResponseEntity<Map> created = restTemplate.exchange(
        "/api/v1/passenger/dnd-windows", HttpMethod.POST,
        new HttpEntity<>(body, bearerHeaders(tokenA)), Map.class);
    long dndId = ((Number) created.getBody().get("id")).longValue();

    ResponseEntity<String> del = restTemplate.exchange(
        "/api/v1/passenger/dnd-windows/" + dndId, HttpMethod.DELETE,
        new HttpEntity<>(bearerHeaders(tokenAdmin)), String.class);
    assertThat(del.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void actuatorMetricsRequiresAdmin() {
    ResponseEntity<String> unauthRes = restTemplate.getForEntity("/actuator/metrics", String.class);
    assertThat(unauthRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    String passengerToken = loginToken("passenger1", "ChangeMe123!");
    ResponseEntity<String> passengerRes = restTemplate.exchange(
        "/actuator/metrics", HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(passengerToken)), String.class);
    assertThat(passengerRes.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    String adminToken = loginToken("admin", "ChangeMe123!");
    ResponseEntity<String> adminRes = restTemplate.exchange(
        "/actuator/metrics", HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(adminToken)), String.class);
    assertThat(adminRes.getStatusCode()).isEqualTo(HttpStatus.OK);
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
