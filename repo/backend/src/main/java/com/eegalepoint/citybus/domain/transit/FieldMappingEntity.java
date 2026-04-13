package com.eegalepoint.citybus.domain.transit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "field_mappings")
public class FieldMappingEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "template_name", nullable = false, length = 128)
  private String templateName;

  @Column(name = "source_field", nullable = false, length = 128)
  private String sourceField;

  @Column(name = "target_field", nullable = false, length = 128)
  private String targetField;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected FieldMappingEntity() {}

  public Long getId() {
    return id;
  }

  public String getTemplateName() {
    return templateName;
  }

  public String getSourceField() {
    return sourceField;
  }

  public String getTargetField() {
    return targetField;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
