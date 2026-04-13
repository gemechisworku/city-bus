package com.eegalepoint.citybus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
  void apiPingReturnsOk() {
    ResponseEntity<String> res = restTemplate.getForEntity("/api/v1/ping", String.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(res.getBody()).contains("ok");
    assertThat(res.getHeaders().getFirst("X-Trace-Id")).isNotBlank();
  }
}
