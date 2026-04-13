package com.eegalepoint.citybus.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RunDiagnosticRequest(
    @NotBlank(message = "reportType is required")
    @Pattern(regexp = "DB_HEALTH|TABLE_STATS|CONNECTION_POOL|FULL",
        message = "reportType must be DB_HEALTH, TABLE_STATS, CONNECTION_POOL, or FULL")
    String reportType) {}
