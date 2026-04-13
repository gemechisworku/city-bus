package com.eegalepoint.citybus.domain.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cleaning_rule_sets")
public class CleaningRuleSetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 128)
  private String name;

  private String description;

  @Column(name = "field_target", nullable = false, length = 128)
  private String fieldTarget;

  @Column(name = "rule_type", nullable = false, length = 32)
  private String ruleType = "REGEX";

  @Column(nullable = false, length = 512)
  private String pattern;

  @Column(nullable = false, length = 512)
  private String replacement = "";

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public CleaningRuleSetEntity() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
    this.updatedAt = Instant.now();
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
    this.updatedAt = Instant.now();
  }

  public String getFieldTarget() {
    return fieldTarget;
  }

  public void setFieldTarget(String fieldTarget) {
    this.fieldTarget = fieldTarget;
    this.updatedAt = Instant.now();
  }

  public String getRuleType() {
    return ruleType;
  }

  public void setRuleType(String ruleType) {
    this.ruleType = ruleType;
    this.updatedAt = Instant.now();
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
    this.updatedAt = Instant.now();
  }

  public String getReplacement() {
    return replacement;
  }

  public void setReplacement(String replacement) {
    this.replacement = replacement;
    this.updatedAt = Instant.now();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    this.updatedAt = Instant.now();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
