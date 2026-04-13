package com.eegalepoint.citybus.domain.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "message_redaction_rules")
public class MessageRedactionRuleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String pattern;

  @Column(nullable = false, length = 255)
  private String replacement = "***";

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected MessageRedactionRuleEntity() {}

  public Long getId() {
    return id;
  }

  public String getPattern() {
    return pattern;
  }

  public String getReplacement() {
    return replacement;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
