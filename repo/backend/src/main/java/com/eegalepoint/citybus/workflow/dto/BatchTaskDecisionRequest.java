package com.eegalepoint.citybus.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record BatchTaskDecisionRequest(
    @NotEmpty(message = "taskIds is required") List<Long> taskIds,
    @NotBlank(message = "action is required")
    @Pattern(regexp = "APPROVE|REJECT|RETURN", message = "action must be APPROVE, REJECT, or RETURN")
    String action,
    String note) {}
