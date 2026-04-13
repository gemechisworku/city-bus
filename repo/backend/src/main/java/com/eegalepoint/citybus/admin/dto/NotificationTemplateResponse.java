package com.eegalepoint.citybus.admin.dto;

import java.time.Instant;

public record NotificationTemplateResponse(
    long id,
    String code,
    String subject,
    String bodyTemplate,
    String channel,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {}
