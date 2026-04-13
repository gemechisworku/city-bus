package com.eegalepoint.citybus.ingestion;

import com.eegalepoint.citybus.domain.transit.SourceImportJobEntity;
import com.eegalepoint.citybus.domain.transit.SourceImportJobRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportJobLifecycleService {

  public static final String SOURCE_CANONICAL_JSON = "CANONICAL_JSON";
  public static final String STATUS_RUNNING = "RUNNING";
  public static final String STATUS_SUCCEEDED = "SUCCEEDED";
  public static final String STATUS_FAILED = "FAILED";

  private final SourceImportJobRepository jobRepository;

  public ImportJobLifecycleService(SourceImportJobRepository jobRepository) {
    this.jobRepository = jobRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SourceImportJobEntity beginJob(String artifactName) {
    SourceImportJobEntity job = new SourceImportJobEntity();
    job.setSourceType(SOURCE_CANONICAL_JSON);
    job.setStatus(STATUS_RUNNING);
    job.setArtifactName(artifactName);
    job.setStartedAt(Instant.now());
    return jobRepository.save(job);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void completeJob(long jobId, int rowCount) {
    SourceImportJobEntity job =
        jobRepository.findById(jobId).orElseThrow(() -> new IllegalStateException("Missing job " + jobId));
    job.setStatus(STATUS_SUCCEEDED);
    job.setRowCount(rowCount);
    job.setCompletedAt(Instant.now());
    jobRepository.save(job);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void failJob(long jobId, String message) {
    SourceImportJobEntity job =
        jobRepository.findById(jobId).orElseThrow(() -> new IllegalStateException("Missing job " + jobId));
    job.setStatus(STATUS_FAILED);
    job.setErrorMessage(message != null ? message : "Import failed");
    job.setCompletedAt(Instant.now());
    jobRepository.save(job);
  }
}
