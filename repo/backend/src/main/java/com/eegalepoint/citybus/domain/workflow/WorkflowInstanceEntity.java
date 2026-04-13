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
@Table(name = "workflow_instances")
public class WorkflowInstanceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "definition_id", nullable = false)
  private WorkflowDefinitionEntity definition;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 32)
  private String status = "OPEN";

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private UserEntity createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to")
  private UserEntity assignedTo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected WorkflowInstanceEntity() {}

  public WorkflowInstanceEntity(WorkflowDefinitionEntity definition, String title, UserEntity createdBy) {
    this.definition = definition;
    this.title = title;
    this.status = definition.getInitialStatus();
    this.createdBy = createdBy;
  }

  public Long getId() {
    return id;
  }

  public WorkflowDefinitionEntity getDefinition() {
    return definition;
  }

  public String getTitle() {
    return title;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public UserEntity getCreatedBy() {
    return createdBy;
  }

  public UserEntity getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(UserEntity assignedTo) {
    this.assignedTo = assignedTo;
    this.updatedAt = Instant.now();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
