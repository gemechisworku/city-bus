package com.eegalepoint.citybus.domain.messaging;

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
@Table(name = "message_queue")
public class MessageQueueEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "message_id", nullable = false)
  private MessageEntity message;

  @Column(nullable = false, length = 32)
  private String status = "QUEUED";

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt = Instant.now();

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected MessageQueueEntity() {}

  public MessageQueueEntity(MessageEntity message) {
    this.message = message;
  }

  public Long getId() {
    return id;
  }

  public MessageEntity getMessage() {
    return message;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public void setSentAt(Instant sentAt) {
    this.sentAt = sentAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
