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
@Table(name = "workflow_tasks")
public class WorkflowTaskEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "instance_id", nullable = false)
  private WorkflowInstanceEntity instance;

  @Column(nullable = false, length = 255)
  private String title;

  private String description;

  @Column(nullable = false, length = 32)
  private String status = "PENDING";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to")
  private UserEntity assignedTo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "decided_by")
  private UserEntity decidedBy;

  @Column(name = "decision_note")
  private String decisionNote;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected WorkflowTaskEntity() {}

  public WorkflowTaskEntity(WorkflowInstanceEntity instance, String title, String description, UserEntity assignedTo) {
    this.instance = instance;
    this.title = title;
    this.description = description;
    this.assignedTo = assignedTo;
  }

  public Long getId() {
    return id;
  }

  public WorkflowInstanceEntity getInstance() {
    return instance;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public UserEntity getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(UserEntity assignedTo) {
    this.assignedTo = assignedTo;
    this.updatedAt = Instant.now();
  }

  public UserEntity getDecidedBy() {
    return decidedBy;
  }

  public void setDecidedBy(UserEntity decidedBy) {
    this.decidedBy = decidedBy;
  }

  public String getDecisionNote() {
    return decisionNote;
  }

  public void setDecisionNote(String decisionNote) {
    this.decisionNote = decisionNote;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
