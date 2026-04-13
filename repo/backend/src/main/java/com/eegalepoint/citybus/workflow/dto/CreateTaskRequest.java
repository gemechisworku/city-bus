package com.eegalepoint.citybus.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
    @NotNull(message = "instanceId is required") Long instanceId,
    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be <= 255 characters")
    String title,
    String description,
    Long assignedToUserId) {}
