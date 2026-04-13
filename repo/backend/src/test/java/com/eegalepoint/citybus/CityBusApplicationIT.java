package com.eegalepoint.citybus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
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
class CityBusApplicationIT {

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

  @Test
  void actuatorHealthIsUp() {
    ResponseEntity<String> res = restTemplate.getForEntity("/actuator/health", String.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(res.getBody()).isNotNull().contains("\"status\":\"UP\"");
  }

  @Test
  void flywaySeededRolesAndAdmin() {
    Integer roles = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
    assertThat(roles).isEqualTo(3);

    Integer admins = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id = u.id "
            + "JOIN roles r ON r.id = ur.role_id WHERE u.username = 'admin' AND r.name = 'ADMIN'",
        Integer.class);
    assertThat(admins).isEqualTo(1);
  }

  @Test
  void apiPingRequiresAuthentication() {
    ResponseEntity<String> res = restTemplate.getForEntity("/api/v1/ping", String.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void loginSuccessAndPingWithBearer() {
    ResponseEntity<Map> login =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "ChangeMe123!"),
            Map.class);
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(login.getBody()).containsKey("accessToken");
    String token = (String) login.getBody().get("accessToken");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<String> ping =
        restTemplate.exchange(
            "/api/v1/ping", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertThat(ping.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(ping.getBody()).contains("ok");
    assertThat(ping.getHeaders().getFirst("X-Trace-Id")).isNotBlank();
  }

  @Test
  void loginBadPasswordReturns401() {
    ResponseEntity<Map> res =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "WrongPass12"),
            Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void loginShortPasswordReturns400() {
    ResponseEntity<Map> res =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "short"),
            Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void meReturnsUserAndRoles() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Map> me =
        restTemplate.exchange(
            "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(me.getBody().get("username")).isEqualTo("admin");
    assertThat(me.getBody()).containsKey("roles");
  }

  @Test
  void rbacPassengerCannotAccessAdminDemo() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Map> res =
        restTemplate.exchange(
            "/api/v1/demo/admin", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void rbacAdminCanAccessAdminDemo() {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Map> res =
        restTemplate.exchange(
            "/api/v1/demo/admin", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(res.getBody().get("scope")).isEqualTo("admin");
  }

  private String loginToken(String user, String pass) {
    ResponseEntity<Map> login =
        restTemplate.postForEntity("/api/v1/auth/login", Map.of("username", user, "password", pass), Map.class);
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (String) login.getBody().get("accessToken");
  }
}
