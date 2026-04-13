package com.eegalepoint.citybus.workflow.dto;

import java.time.Instant;
import java.util.List;

public record TaskResponse(
    long id,
    long instanceId,
    List<Long> predecessorTaskIds,
    String title,
    String description,
    String status,
    String assignedToUsername,
    String decidedByUsername,
    String decisionNote,
    Instant createdAt,
    Instant updatedAt) {}
