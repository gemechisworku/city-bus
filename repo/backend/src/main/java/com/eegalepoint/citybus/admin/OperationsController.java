package com.eegalepoint.citybus.admin;

import com.eegalepoint.citybus.admin.dto.CreateAlertRequest;
import com.eegalepoint.citybus.admin.dto.DiagnosticReportResponse;
import com.eegalepoint.citybus.admin.dto.RunDiagnosticRequest;
import com.eegalepoint.citybus.admin.dto.SystemAlertResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public class OperationsController {

  private final OperationsService operationsService;

  public OperationsController(OperationsService operationsService) {
    this.operationsService = operationsService;
  }

  // ── Alerts ──

  @GetMapping("/alerts")
  public List<SystemAlertResponse> listAlerts(
      @RequestParam(value = "unacknowledged", required = false, defaultValue = "false") boolean unacknowledged) {
    return operationsService.listAlerts(unacknowledged);
  }

  @PostMapping(path = "/alerts", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public SystemAlertResponse createAlert(@Valid @RequestBody CreateAlertRequest req) {
    return operationsService.createAlert(req);
  }

  @PostMapping("/alerts/{id}/acknowledge")
  public SystemAlertResponse acknowledgeAlert(@PathVariable("id") long id) {
    return operationsService.acknowledgeAlert(id);
  }

  // ── Diagnostics ──

  @GetMapping("/diagnostics")
  public List<DiagnosticReportResponse> listDiagnostics() {
    return operationsService.listDiagnostics();
  }

  @PostMapping(path = "/diagnostics", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public DiagnosticReportResponse runDiagnostic(@Valid @RequestBody RunDiagnosticRequest req) {
    return operationsService.runDiagnostic(req);
  }
}
