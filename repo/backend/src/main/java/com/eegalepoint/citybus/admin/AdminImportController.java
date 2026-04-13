package com.eegalepoint.citybus.admin;

import com.eegalepoint.citybus.ingestion.CanonicalImportService;
import com.eegalepoint.citybus.ingestion.HtmlImportService;
import com.eegalepoint.citybus.transit.dto.CanonicalImportRequest;
import com.eegalepoint.citybus.transit.dto.ImportJobSummaryResponse;
import com.eegalepoint.citybus.transit.dto.ImportRunResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/admin/imports", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminImportController {

  private final CanonicalImportService canonicalImportService;
  private final HtmlImportService htmlImportService;

  public AdminImportController(
      CanonicalImportService canonicalImportService,
      HtmlImportService htmlImportService) {
    this.canonicalImportService = canonicalImportService;
    this.htmlImportService = htmlImportService;
  }

  @PostMapping(path = "/run", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ImportRunResponse run(@Valid @RequestBody CanonicalImportRequest body) {
    return canonicalImportService.run(body);
  }

  @PostMapping(path = "/run-html", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public ImportRunResponse runHtml(@RequestBody Map<String, String> body) {
    String html = body.get("html");
    String templateName = body.getOrDefault("templateName", "html-import");
    CanonicalImportRequest request = htmlImportService.parseHtml(html, templateName);
    return canonicalImportService.run(request);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<ImportJobSummaryResponse> list() {
    return canonicalImportService.listJobs();
  }
}
