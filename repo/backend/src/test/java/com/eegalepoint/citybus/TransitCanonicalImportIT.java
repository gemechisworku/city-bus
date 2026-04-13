package com.eegalepoint.citybus;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
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
class TransitCanonicalImportIT {

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

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void adminCanImportCanonicalJsonAndQueryRoutes() throws Exception {
    String token = loginToken("admin", "ChangeMe123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> payload =
        objectMapper.readValue(
            new ClassPathResource("canonical-import-min.json").getInputStream(),
            new TypeReference<Map<String, Object>>() {});

    ResponseEntity<Map> run =
        restTemplate.postForEntity("/api/v1/admin/imports/run", new HttpEntity<>(payload, headers), Map.class);
    assertThat(run.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(run.getBody()).containsEntry("status", "SUCCEEDED");
    assertThat(run.getBody()).containsKey("jobId");

    ResponseEntity<List> routes =
        restTemplate.exchange(
            "/api/v1/routes", HttpMethod.GET, new HttpEntity<>(headers), List.class);
    assertThat(routes.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(routes.getBody()).hasSize(1);

    long routeId =
        jdbcTemplate.queryForObject("SELECT id FROM routes WHERE code = 'R-IT-1'", Long.class);

    ResponseEntity<Map> detail =
        restTemplate.exchange(
            "/api/v1/routes/" + routeId, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(detail.getBody().get("code")).isEqualTo("R-IT-1");
    assertThat(detail.getBody()).containsKey("stops");

    Integer routeRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM routes", Integer.class);
    assertThat(routeRows).isEqualTo(1);
    Integer jobs = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM source_import_jobs WHERE status = 'SUCCEEDED'", Integer.class);
    assertThat(jobs).isGreaterThanOrEqualTo(1);
  }

  @Test
  void passengerCannotRunImport() {
    String token = loginToken("passenger1", "ChangeMe123!");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    String body = "{\"templateName\":\"DEFAULT_V1\",\"routes\":[]}";
    ResponseEntity<Map> res =
        restTemplate.postForEntity("/api/v1/admin/imports/run", new HttpEntity<>(body, headers), Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  private String loginToken(String user, String pass) {
    ResponseEntity<Map> login =
        restTemplate.postForEntity("/api/v1/auth/login", Map.of("username", user, "password", pass), Map.class);
    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (String) login.getBody().get("accessToken");
  }
}
