package com.eegalepoint.citybus.domain.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinitionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 128)
  private String name;

  private String description;

  @Column(name = "initial_status", nullable = false, length = 32)
  private String initialStatus = "OPEN";

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "approval_mode", nullable = false, length = 32)
  private String approvalMode = "ALL";

  @Column(name = "required_approvals", nullable = false)
  private int requiredApprovals = 1;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected WorkflowDefinitionEntity() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getInitialStatus() {
    return initialStatus;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getApprovalMode() {
    return approvalMode;
  }

  public void setApprovalMode(String approvalMode) {
    this.approvalMode = approvalMode;
  }

  public int getRequiredApprovals() {
    return requiredApprovals;
  }

  public void setRequiredApprovals(int requiredApprovals) {
    this.requiredApprovals = requiredApprovals;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
