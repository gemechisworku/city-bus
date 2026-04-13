package com.eegalepoint.citybus.domain.workflow;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_task_dependencies")
public class WorkflowTaskDependencyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false)
  private WorkflowTaskEntity task;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "depends_on_task_id", nullable = false)
  private WorkflowTaskEntity dependsOn;

  protected WorkflowTaskDependencyEntity() {}

  public WorkflowTaskDependencyEntity(WorkflowTaskEntity task, WorkflowTaskEntity dependsOn) {
    this.task = task;
    this.dependsOn = dependsOn;
  }

  public Long getId() {
    return id;
  }

  public WorkflowTaskEntity getTask() {
    return task;
  }

  public WorkflowTaskEntity getDependsOn() {
    return dependsOn;
  }
}
