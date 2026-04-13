package com.eegalepoint.citybus.domain.operations;

import com.eegalepoint.citybus.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "diagnostic_reports")
public class DiagnosticReportEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "report_type", nullable = false, length = 64)
  private String reportType;

  @Column(nullable = false, length = 32)
  private String status = "RUNNING";

  private String summary;

  private String detail;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "triggered_by")
  private UserEntity triggeredBy;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt = Instant.now();

  @Column(name = "completed_at")
  private Instant completedAt;

  public DiagnosticReportEntity() {}

  public DiagnosticReportEntity(String reportType, UserEntity triggeredBy) {
    this.reportType = reportType;
    this.triggeredBy = triggeredBy;
  }

  public Long getId() {
    return id;
  }

  public String getReportType() {
    return reportType;
  }

  public String getStatus() {
    return status;
  }

  public String getSummary() {
    return summary;
  }

  public String getDetail() {
    return detail;
  }

  public UserEntity getTriggeredBy() {
    return triggeredBy;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void complete(String summary, String detail) {
    this.status = "COMPLETED";
    this.summary = summary;
    this.detail = detail;
    this.completedAt = Instant.now();
  }

  public void fail(String summary) {
    this.status = "FAILED";
    this.summary = summary;
    this.completedAt = Instant.now();
  }
}
