package com.eegalepoint.citybus.admin.dto;

import java.time.Instant;

public record SystemAlertResponse(
    long id,
    String severity,
    String source,
    String title,
    String detail,
    boolean acknowledged,
    String acknowledgedByUsername,
    Instant acknowledgedAt,
    Instant createdAt) {}
