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
class PassengerMessagingIT {

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
  void seedTransitData() {
    jdbcTemplate.update("DELETE FROM message_queue_attempts");
    jdbcTemplate.update("DELETE FROM message_queue");
    jdbcTemplate.update("DELETE FROM messages");
    jdbcTemplate.update("DELETE FROM passenger_checkins");
    jdbcTemplate.update("DELETE FROM passenger_reservations");
    jdbcTemplate.update("DELETE FROM reminder_preferences");
    jdbcTemplate.update("DELETE FROM do_not_disturb_windows");
    jdbcTemplate.update("DELETE FROM route_stops");
    jdbcTemplate.update("DELETE FROM schedules");
    jdbcTemplate.update("DELETE FROM route_versions");
    jdbcTemplate.update("DELETE FROM routes");
    jdbcTemplate.update("DELETE FROM stop_versions");
    jdbcTemplate.update("DELETE FROM stops");

    jdbcTemplate.update("INSERT INTO routes (code) VALUES ('PM-R1')");
    Long routeId =
        jdbcTemplate.queryForObject("SELECT id FROM routes WHERE code = 'PM-R1'", Long.class);
    jdbcTemplate.update(
        "INSERT INTO route_versions (route_id, version_number, name) VALUES (?, 1, 'Test Route')",
        routeId);
    Long rvId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM route_versions WHERE route_id = ? AND version_number = 1",
            Long.class,
            routeId);

    jdbcTemplate.update("INSERT INTO stops (code) VALUES ('PM-S1')");
    stopId = jdbcTemplate.queryForObject("SELECT id FROM stops WHERE code = 'PM-S1'", Long.class);
    jdbcTemplate.update(
        "INSERT INTO stop_versions (stop_id, version_number, name, latitude, longitude) "
            + "VALUES (?, 1, 'Central Station', 40.712776, -74.005974)",
        stopId);

    jdbcTemplate.update(
        "INSERT INTO schedules (route_version_id, trip_code, departure_time) VALUES (?, 'TRIP-01', '08:30')",
        rvId);
    scheduleId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM schedules WHERE trip_code = 'TRIP-01'", Long.class);
  }

  @Test
  void reservationsRequireAuth() {
    ResponseEntity<String> res =
        restTemplate.getForEntity("/api/v1/passenger/reservations", String.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void passengerCanCreateAndListReservation() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of("scheduleId", scheduleId, "stopId", stopId);
    ResponseEntity<Map> create =
        restTemplate.exchange(
            "/api/v1/passenger/reservations",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Map.class);
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(create.getBody()).containsEntry("status", "PENDING");
    assertThat(create.getBody()).containsEntry("stopCode", "PM-S1");

    ResponseEntity<List> list =
        restTemplate.exchange(
            "/api/v1/passenger/reservations",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class);
    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody()).hasSize(1);
  }

  @Test
  void passengerCanCancelReservation() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of("scheduleId", scheduleId, "stopId", stopId);
    ResponseEntity<Map> create =
        restTemplate.exchange(
            "/api/v1/passenger/reservations",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Map.class);
    long resId = ((Number) create.getBody().get("id")).longValue();

    Map<String, Object> update = Map.of("status", "CANCELLED");
    ResponseEntity<Map> cancel =
        restTemplate.exchange(
            "/api/v1/passenger/reservations/" + resId,
            HttpMethod.PUT,
            new HttpEntity<>(update, headers),
            Map.class);
    assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancel.getBody()).containsEntry("status", "CANCELLED");
  }

  @Test
  void passengerCanCheckin() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of("stopId", stopId);
    ResponseEntity<Map> checkin =
        restTemplate.exchange(
            "/api/v1/passenger/checkins",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Map.class);
    assertThat(checkin.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(checkin.getBody()).containsEntry("stopCode", "PM-S1");

    ResponseEntity<List> list =
        restTemplate.exchange(
            "/api/v1/passenger/checkins",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class);
    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody()).hasSize(1);
  }

  @Test
  void passengerCanCheckinWithReservation() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> resBody = Map.of("scheduleId", scheduleId, "stopId", stopId);
    ResponseEntity<Map> create =
        restTemplate.exchange(
            "/api/v1/passenger/reservations",
            HttpMethod.POST,
            new HttpEntity<>(resBody, headers),
            Map.class);
    long resId = ((Number) create.getBody().get("id")).longValue();

    Map<String, Object> body = Map.of("stopId", stopId, "reservationId", resId);
    ResponseEntity<Map> checkin =
        restTemplate.exchange(
            "/api/v1/passenger/checkins",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Map.class);
    assertThat(checkin.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(((Number) checkin.getBody().get("reservationId")).longValue()).isEqualTo(resId);
  }

  @Test
  void reminderPreferencesDefaultValues() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    ResponseEntity<Map> get =
        restTemplate.exchange(
            "/api/v1/passenger/reminder-preferences",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(get.getBody()).containsEntry("enabled", true);
    assertThat(get.getBody()).containsEntry("minutesBefore", 10);
    assertThat(get.getBody()).containsEntry("channel", "IN_APP");
  }

  @Test
  void passengerCanUpdateReminderPreferences() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of("enabled", false, "minutesBefore", 30, "channel", "EMAIL");
    ResponseEntity<Map> put =
        restTemplate.exchange(
            "/api/v1/passenger/reminder-preferences",
            HttpMethod.PUT,
            new HttpEntity<>(body, headers),
            Map.class);
    assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(put.getBody()).containsEntry("enabled", false);
    assertThat(put.getBody()).containsEntry("minutesBefore", 30);
    assertThat(put.getBody()).containsEntry("channel", "EMAIL");

    ResponseEntity<Map> verify =
        restTemplate.exchange(
            "/api/v1/passenger/reminder-preferences",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(verify.getBody()).containsEntry("enabled", false);
  }

  @Test
  void reservationCreatesMessage() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> body = Map.of("scheduleId", scheduleId, "stopId", stopId);
    restTemplate.exchange(
        "/api/v1/passenger/reservations",
        HttpMethod.POST,
        new HttpEntity<>(body, headers),
        Map.class);

    ResponseEntity<List> messages =
        restTemplate.exchange(
            "/api/v1/messages", HttpMethod.GET, new HttpEntity<>(headers), List.class);
    assertThat(messages.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(messages.getBody()).isNotEmpty();

    Integer queueCount =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message_queue", Integer.class);
    assertThat(queueCount).isGreaterThanOrEqualTo(1);
  }

  @Test
  void messageMarkReadFlow() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    Map<String, Object> msgBody = Map.of("subject", "Test", "body", "Hello world");
    ResponseEntity<Map> created =
        restTemplate.exchange(
            "/api/v1/messages",
            HttpMethod.POST,
            new HttpEntity<>(msgBody, headers),
            Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    long msgId = ((Number) created.getBody().get("id")).longValue();
    assertThat(created.getBody()).containsEntry("read", false);

    ResponseEntity<Map> detail =
        restTemplate.exchange(
            "/api/v1/messages/" + msgId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(detail.getBody()).containsEntry("subject", "Test");

    ResponseEntity<Map> read =
        restTemplate.exchange(
            "/api/v1/messages/" + msgId + "/read",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(read.getBody()).containsEntry("read", true);
  }

  @Test
  void messagesRequireAuth() {
    ResponseEntity<String> res = restTemplate.getForEntity("/api/v1/messages", String.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void dispatcherCannotAccessPassengerReservations() {
    String token = loginToken("dispatcher1", "ChangeMe123!");
    HttpHeaders headers = bearerHeaders(token);

    ResponseEntity<String> res =
        restTemplate.exchange(
            "/api/v1/passenger/reservations",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  private String loginToken(String user, String pass) {
    ResponseEntity<Map> login =
        restTemplate.postForEntity(
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
