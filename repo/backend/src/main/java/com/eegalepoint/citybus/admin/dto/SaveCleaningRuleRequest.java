package com.eegalepoint.citybus.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveCleaningRuleRequest(
    @NotBlank(message = "name is required")
    @Size(max = 128, message = "name must be <= 128 characters")
    String name,
    String description,
    @NotBlank(message = "fieldTarget is required") String fieldTarget,
    @NotBlank(message = "pattern is required")
    @Size(max = 512, message = "pattern must be <= 512 characters")
    String pattern,
    String replacement,
    @NotNull(message = "enabled is required") Boolean enabled) {}
