package com.eegalepoint.citybus.ingestion;

import com.eegalepoint.citybus.domain.transit.FieldMappingRepository;
import com.eegalepoint.citybus.domain.transit.SourceImportJobRepository;
import com.eegalepoint.citybus.transit.dto.CanonicalImportRequest;
import com.eegalepoint.citybus.transit.dto.ImportJobSummaryResponse;
import com.eegalepoint.citybus.transit.dto.ImportRunResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CanonicalImportService {

  private final FieldMappingRepository fieldMappingRepository;
  private final SourceImportJobRepository sourceImportJobRepository;
  private final ImportJobLifecycleService jobLifecycleService;
  private final CanonicalImportTransactionalService transactionalImportService;

  public CanonicalImportService(
      FieldMappingRepository fieldMappingRepository,
      SourceImportJobRepository sourceImportJobRepository,
      ImportJobLifecycleService jobLifecycleService,
      CanonicalImportTransactionalService transactionalImportService) {
    this.fieldMappingRepository = fieldMappingRepository;
    this.sourceImportJobRepository = sourceImportJobRepository;
    this.jobLifecycleService = jobLifecycleService;
    this.transactionalImportService = transactionalImportService;
  }

  @Transactional(readOnly = true)
  public List<ImportJobSummaryResponse> listJobs() {
    return sourceImportJobRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(
            j ->
                new ImportJobSummaryResponse(
                    j.getId(),
                    j.getSourceType(),
                    j.getStatus(),
                    j.getArtifactName(),
                    j.getRowCount(),
                    j.getErrorMessage(),
                    j.getCreatedAt(),
                    j.getStartedAt(),
                    j.getCompletedAt()))
        .toList();
  }

  public ImportRunResponse run(CanonicalImportRequest request) {
    if (!fieldMappingRepository.existsByTemplateName(request.templateName())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unknown templateName: " + request.templateName());
    }
    var job = jobLifecycleService.beginJob(request.templateName());
    try {
      int rows = transactionalImportService.importCanonical(request);
      jobLifecycleService.completeJob(job.getId(), rows);
      return new ImportRunResponse(
          job.getId(), ImportJobLifecycleService.STATUS_SUCCEEDED, rows, null);
    } catch (RuntimeException ex) {
      String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
      jobLifecycleService.failJob(job.getId(), msg);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg, ex);
    }
  }
}
