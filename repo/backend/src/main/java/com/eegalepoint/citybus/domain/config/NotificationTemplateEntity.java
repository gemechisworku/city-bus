package com.eegalepoint.citybus.domain.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplateEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String code;

  @Column(nullable = false)
  private String subject;

  @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
  private String bodyTemplate;

  @Column(nullable = false, length = 32)
  private String channel = "IN_APP";

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public NotificationTemplateEntity() {}

  public NotificationTemplateEntity(String code, String subject, String bodyTemplate, String channel, boolean enabled) {
    this.code = code;
    this.subject = subject;
    this.bodyTemplate = bodyTemplate;
    this.channel = channel;
    this.enabled = enabled;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
    this.updatedAt = Instant.now();
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
    this.updatedAt = Instant.now();
  }

  public String getBodyTemplate() {
    return bodyTemplate;
  }

  public void setBodyTemplate(String bodyTemplate) {
    this.bodyTemplate = bodyTemplate;
    this.updatedAt = Instant.now();
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
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
