package com.eegalepoint.citybus.domain.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "field_standard_dictionaries")
public class FieldStandardDictionaryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "field_name", nullable = false, length = 128)
  private String fieldName;

  @Column(name = "canonical_value", nullable = false, length = 255)
  private String canonicalValue;

  private String aliases;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public FieldStandardDictionaryEntity() {}

  public Long getId() {
    return id;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
    this.updatedAt = Instant.now();
  }

  public String getCanonicalValue() {
    return canonicalValue;
  }

  public void setCanonicalValue(String canonicalValue) {
    this.canonicalValue = canonicalValue;
    this.updatedAt = Instant.now();
  }

  public String getAliases() {
    return aliases;
  }

  public void setAliases(String aliases) {
    this.aliases = aliases;
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
