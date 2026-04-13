package com.eegalepoint.citybus.admin;

import com.eegalepoint.citybus.admin.dto.CreateAlertRequest;
import com.eegalepoint.citybus.admin.dto.DiagnosticReportResponse;
import com.eegalepoint.citybus.admin.dto.RunDiagnosticRequest;
import com.eegalepoint.citybus.admin.dto.SystemAlertResponse;
import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.domain.operations.DiagnosticReportEntity;
import com.eegalepoint.citybus.domain.operations.DiagnosticReportRepository;
import com.eegalepoint.citybus.domain.operations.SystemAlertEntity;
import com.eegalepoint.citybus.domain.operations.SystemAlertRepository;
import com.eegalepoint.citybus.repo.UserRepository;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperationsService {

  private final SystemAlertRepository alertRepository;
  private final DiagnosticReportRepository diagnosticRepository;
  private final UserRepository userRepository;
  private final JdbcTemplate jdbcTemplate;

  public OperationsService(
      SystemAlertRepository alertRepository,
      DiagnosticReportRepository diagnosticRepository,
      UserRepository userRepository,
      JdbcTemplate jdbcTemplate) {
    this.alertRepository = alertRepository;
    this.diagnosticRepository = diagnosticRepository;
    this.userRepository = userRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  // ── Alerts ──

  @Transactional(readOnly = true)
  public List<SystemAlertResponse> listAlerts(boolean unacknowledgedOnly) {
    List<SystemAlertEntity> alerts = unacknowledgedOnly
        ? alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc()
        : alertRepository.findAllByOrderByCreatedAtDesc();
    return alerts.stream().map(this::toAlertResponse).toList();
  }

  @Transactional
  public SystemAlertResponse createAlert(CreateAlertRequest req) {
    SystemAlertEntity alert = alertRepository.save(
        new SystemAlertEntity(req.severity(), req.source(), req.title(), req.detail()));
    return toAlertResponse(alert);
  }

  @Transactional
  public SystemAlertResponse acknowledgeAlert(long id) {
    UserEntity user = currentUser();
    SystemAlertEntity alert = alertRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
    if (alert.isAcknowledged()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert already acknowledged");
    }
    alert.acknowledge(user);
    alertRepository.save(alert);
    return toAlertResponse(alert);
  }

  private SystemAlertResponse toAlertResponse(SystemAlertEntity e) {
    return new SystemAlertResponse(
        e.getId(), e.getSeverity(), e.getSource(), e.getTitle(), e.getDetail(),
        e.isAcknowledged(),
        e.getAcknowledgedBy() != null ? e.getAcknowledgedBy().getUsername() : null,
        e.getAcknowledgedAt(),
        e.getCreatedAt());
  }

  // ── Diagnostics ──

  @Transactional(readOnly = true)
  public List<DiagnosticReportResponse> listDiagnostics() {
    return diagnosticRepository.findAllByOrderByStartedAtDesc()
        .stream().map(this::toDiagnosticResponse).toList();
  }

  @Transactional
  public DiagnosticReportResponse runDiagnostic(RunDiagnosticRequest req) {
    UserEntity user = currentUser();
    DiagnosticReportEntity report = diagnosticRepository.save(
        new DiagnosticReportEntity(req.reportType(), user));

    try {
      String summary;
      String detail;
      switch (req.reportType()) {
        case "DB_HEALTH" -> {
          summary = runDbHealth();
          detail = summary;
        }
        case "TABLE_STATS" -> {
          detail = runTableStats();
          summary = "Table statistics collected";
        }
        case "CONNECTION_POOL" -> {
          detail = runConnectionPoolCheck();
          summary = "Connection pool healthy";
        }
        case "FULL" -> {
          StringJoiner sj = new StringJoiner("\n---\n");
          sj.add("DB_HEALTH: " + runDbHealth());
          sj.add("TABLE_STATS:\n" + runTableStats());
          sj.add("CONNECTION_POOL:\n" + runConnectionPoolCheck());
          detail = sj.toString();
          summary = "Full diagnostic completed";
        }
        default -> {
          summary = "Unknown report type";
          detail = "";
        }
      }
      report.complete(summary, detail);
    } catch (Exception ex) {
      report.fail("Diagnostic failed: " + ex.getMessage());
    }
    diagnosticRepository.save(report);
    return toDiagnosticResponse(report);
  }

  private String runDbHealth() {
    Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
    return "Database reachable (SELECT 1 = " + result + "). " + version;
  }

  private String runTableStats() {
    List<String> rows = jdbcTemplate.query(
        "SELECT tablename, n_live_tup FROM pg_stat_user_tables ORDER BY n_live_tup DESC LIMIT 30",
        (rs, i) -> rs.getString("tablename") + ": " + rs.getLong("n_live_tup") + " rows");
    return String.join("\n", rows);
  }

  private String runConnectionPoolCheck() {
    Integer active = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND state = 'active'",
        Integer.class);
    Integer total = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()",
        Integer.class);
    return "Active connections: " + active + ", Total connections: " + total;
  }

  private DiagnosticReportResponse toDiagnosticResponse(DiagnosticReportEntity e) {
    return new DiagnosticReportResponse(
        e.getId(), e.getReportType(), e.getStatus(),
        e.getSummary(), e.getDetail(),
        e.getTriggeredBy() != null ? e.getTriggeredBy().getUsername() : null,
        e.getStartedAt(), e.getCompletedAt());
  }

  private UserEntity currentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
  }
}
