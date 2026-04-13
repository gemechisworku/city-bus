package com.eegalepoint.citybus.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAlertRequest(
    @NotBlank(message = "severity is required")
    @Pattern(regexp = "INFO|WARN|ERROR|CRITICAL", message = "severity must be INFO, WARN, ERROR, or CRITICAL")
    String severity,
    @NotBlank(message = "source is required")
    @Size(max = 128, message = "source must be <= 128 characters")
    String source,
    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be <= 255 characters")
    String title,
    String detail) {}
