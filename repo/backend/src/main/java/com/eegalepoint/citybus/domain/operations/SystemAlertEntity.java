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
@Table(name = "system_alerts")
public class SystemAlertEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 16)
  private String severity = "INFO";

  @Column(nullable = false, length = 128)
  private String source;

  @Column(nullable = false, length = 255)
  private String title;

  private String detail;

  @Column(nullable = false)
  private boolean acknowledged = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "acknowledged_by")
  private UserEntity acknowledgedBy;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public SystemAlertEntity() {}

  public SystemAlertEntity(String severity, String source, String title, String detail) {
    this.severity = severity;
    this.source = source;
    this.title = title;
    this.detail = detail;
  }

  public Long getId() {
    return id;
  }

  public String getSeverity() {
    return severity;
  }

  public String getSource() {
    return source;
  }

  public String getTitle() {
    return title;
  }

  public String getDetail() {
    return detail;
  }

  public boolean isAcknowledged() {
    return acknowledged;
  }

  public UserEntity getAcknowledgedBy() {
    return acknowledgedBy;
  }

  public Instant getAcknowledgedAt() {
    return acknowledgedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void acknowledge(UserEntity user) {
    this.acknowledged = true;
    this.acknowledgedBy = user;
    this.acknowledgedAt = Instant.now();
  }
}
