package com.eegalepoint.citybus.admin.dto;

import java.time.Instant;

public record DiagnosticReportResponse(
    long id,
    String reportType,
    String status,
    String summary,
    String detail,
    String triggeredByUsername,
    Instant startedAt,
    Instant completedAt) {}
