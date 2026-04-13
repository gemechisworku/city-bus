package com.eegalepoint.citybus.domain.config;

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
@Table(name = "cleaning_audit_logs")
public class CleaningAuditLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "rule_id", nullable = false)
  private CleaningRuleSetEntity rule;

  @Column(name = "original_value", nullable = false)
  private String originalValue;

  @Column(name = "cleaned_value", nullable = false)
  private String cleanedValue;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "applied_by")
  private UserEntity appliedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected CleaningAuditLogEntity() {}

  public CleaningAuditLogEntity(CleaningRuleSetEntity rule, String originalValue, String cleanedValue, UserEntity appliedBy) {
    this.rule = rule;
    this.originalValue = originalValue;
    this.cleanedValue = cleanedValue;
    this.appliedBy = appliedBy;
  }

  public Long getId() {
    return id;
  }

  public CleaningRuleSetEntity getRule() {
    return rule;
  }

  public String getOriginalValue() {
    return originalValue;
  }

  public String getCleanedValue() {
    return cleanedValue;
  }

  public UserEntity getAppliedBy() {
    return appliedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
