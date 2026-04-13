package com.eegalepoint.citybus.domain.workflow;

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
@Table(name = "workflow_escalations")
public class WorkflowEscalationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false)
  private WorkflowTaskEntity task;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "escalated_to", nullable = false)
  private UserEntity escalatedTo;

  @Column(nullable = false)
  private String reason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected WorkflowEscalationEntity() {}

  public WorkflowEscalationEntity(WorkflowTaskEntity task, UserEntity escalatedTo, String reason) {
    this.task = task;
    this.escalatedTo = escalatedTo;
    this.reason = reason;
  }

  public Long getId() {
    return id;
  }

  public WorkflowTaskEntity getTask() {
    return task;
  }

  public UserEntity getEscalatedTo() {
    return escalatedTo;
  }

  public String getReason() {
    return reason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
