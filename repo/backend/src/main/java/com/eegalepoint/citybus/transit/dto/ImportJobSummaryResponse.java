package com.eegalepoint.citybus.transit.dto;

import java.time.Instant;

public record ImportJobSummaryResponse(
    long id,
    String sourceType,
    String status,
    String artifactName,
    Integer rowCount,
    String errorMessage,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt) {}
