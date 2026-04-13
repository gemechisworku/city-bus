package com.eegalepoint.citybus.messaging.dto;

import java.time.Instant;

public record MessageResponse(
    long id,
    String subject,
    String body,
    boolean read,
    Instant createdAt) {}
