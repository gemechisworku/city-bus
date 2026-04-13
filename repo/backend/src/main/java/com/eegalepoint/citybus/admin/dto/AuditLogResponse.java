package com.eegalepoint.citybus.admin.dto;

import java.time.Instant;

public record AuditLogResponse(
    long id,
    Long userId,
    String usernameAttempt,
    boolean success,
    String ipAddress,
    Instant createdAt) {}
