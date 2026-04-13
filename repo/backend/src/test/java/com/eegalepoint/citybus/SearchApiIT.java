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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class SearchApiIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("citybus")
      .withUsername("citybus")
      .withPassword("citybus");

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Autowired
  TestRestTemplate restTemplate;

  @BeforeEach
  void seedTransitRows() {
    jdbcTemplate.update("DELETE FROM route_stops");
    jdbcTemplate.update("DELETE FROM schedules");
    jdbcTemplate.update("DELETE FROM route_versions");
    jdbcTemplate.update("DELETE FROM routes");
    jdbcTemplate.update("DELETE FROM stop_versions");
    jdbcTemplate.update("DELETE FROM stops");
    jdbcTemplate.update("DELETE FROM stop_popularity_metrics");
    jdbcTemplate.update("DELETE FROM search_events");

    jdbcTemplate.update("INSERT INTO routes (code) VALUES ('SRCH-R1')");
    Long routeId =
        jdbcTemplate.queryForObject("SELECT id FROM routes WHERE code = 'SRCH-R1'", Long.class);
    jdbcTemplate.update(
        "INSERT INTO route_versions (route_id, version_number, name) VALUES (?, ?, ?)",
        routeId,
        1,
        "Sunrise Express Line");

    jdbcTemplate.update("INSERT INTO stops (code) VALUES ('SRCH-S1')");
    Long stopId =
        jdbcTemplate.queryForObject("SELECT id FROM stops WHERE code = 'SRCH-S1'", Long.class);
    jdbcTemplate.update(
        "INSERT INTO stop_versions (stop_id, version_number, name, latitude, longitude) VALUES (?, ?, ?, ?, ?)",
        stopId,
        1,
        "Oak Street Terminal",
        new java.math.BigDecimal("40.712776"),
        new java.math.BigDecimal("-74.005974"));
  }

  @Test
  void suggestionsAndResultsRequireAuth() {
    ResponseEntity<String> res =
        restTemplate.getForEntity("/api/v1/search/suggestions?q=Sun", String.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shortQueryReturns400() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Map> res =
        restTemplate.exchange(
            "/api/v1/search/suggestions?q=x",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void suggestionsReturnRoutesAndStops() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    ResponseEntity<List> sun =
        restTemplate.exchange(
            "/api/v1/search/suggestions?q=Sun&limit=10",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class);
    assertThat(sun.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(sun.getBody()).isNotEmpty();

    ResponseEntity<List> oak =
        restTemplate.exchange(
            "/api/v1/search/suggestions?q=Oak",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class);
    assertThat(oak.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(oak.getBody()).isNotEmpty();

    Integer events =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM search_events WHERE scope = 'SUGGESTIONS'", Integer.class);
    assertThat(events).isGreaterThanOrEqualTo(2);
  }

  @Test
  void resultsIncrementImpressionsAndLogSearchEvent() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    ResponseEntity<List> res =
        restTemplate.exchange(
            "/api/v1/search/results?q=Oak&limit=5",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            List.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(res.getBody()).isNotEmpty();

    Long imp =
        jdbcTemplate.queryForObject(
            "SELECT impression_count FROM stop_popularity_metrics spm "
                + "JOIN stops s ON s.id = spm.stop_id WHERE s.code = 'SRCH-S1'",
            Long.class);
    assertThat(imp).isEqualTo(1L);

    Integer resultEvents =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM search_events WHERE scope = 'RESULTS'", Integer.class);
    assertThat(resultEvents).isGreaterThanOrEqualTo(1);
  }

  @Test
  void stopDetailById() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    Long stopId =
        jdbcTemplate.queryForObject("SELECT id FROM stops WHERE code = 'SRCH-S1'", Long.class);

    ResponseEntity<Map> res =
        restTemplate.exchange(
            "/api/v1/stops/" + stopId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(res.getBody().get("code")).isEqualTo("SRCH-S1");
    assertThat(res.getBody().get("name")).isEqualTo("Oak Street Terminal");
  }

  private String loginToken(String user, String pass) {
    ResponseEntity<Map> login =
        restTemplate.postForEntity("/api/v1/auth/login", Map.of("username", user, "password", pass), Map.class);
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (String) login.getBody().get("accessToken");
  }
}
