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
@Table(name = "message_queue_attempts")
public class MessageQueueAttemptEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "queue_id", nullable = false)
  private MessageQueueEntity queueEntry;

  @Column(name = "attempted_at", nullable = false)
  private Instant attemptedAt = Instant.now();

  @Column(nullable = false, length = 32)
  private String outcome;

  @Column(name = "error_message")
  private String errorMessage;

  protected MessageQueueAttemptEntity() {}

  public MessageQueueAttemptEntity(MessageQueueEntity queueEntry, String outcome, String errorMessage) {
    this.queueEntry = queueEntry;
    this.outcome = outcome;
    this.errorMessage = errorMessage;
  }

  public Long getId() {
    return id;
  }

  public MessageQueueEntity getQueueEntry() {
    return queueEntry;
  }

  public Instant getAttemptedAt() {
    return attemptedAt;
  }

  public String getOutcome() {
    return outcome;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
