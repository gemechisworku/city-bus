package com.eegalepoint.citybus.workflow.dto;

import java.time.Instant;

public record TaskResponse(
    long id,
    long instanceId,
    String title,
    String description,
    String status,
    String assignedToUsername,
    String decidedByUsername,
    String decisionNote,
    Instant createdAt,
    Instant updatedAt) {}
