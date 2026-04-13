package com.eegalepoint.citybus.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateTaskRequest(
    @NotNull(message = "instanceId is required") Long instanceId,
    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be <= 255 characters")
    String title,
    String description,
    Long assignedToUserId,
    /** Legacy single predecessor; merged with {@code predecessorTaskIds}. */
    Long predecessorTaskId,
    /** All listed tasks (same instance) must be APPROVED before this task can be decided. */
    List<Long> predecessorTaskIds) {}
