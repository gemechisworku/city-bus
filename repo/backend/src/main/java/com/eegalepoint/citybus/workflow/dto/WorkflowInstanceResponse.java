package com.eegalepoint.citybus.workflow.dto;

import java.time.Instant;
import java.util.List;

public record WorkflowInstanceResponse(
    long id,
    String definitionName,
    String title,
    String status,
    String createdByUsername,
    String assignedToUsername,
    List<TaskResponse> tasks,
    Instant createdAt,
    Instant updatedAt) {}
