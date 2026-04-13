package com.eegalepoint.citybus.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveNotificationTemplateRequest(
    @NotBlank(message = "code is required")
    @Size(max = 64, message = "code must be <= 64 characters")
    String code,
    @NotBlank(message = "subject is required")
    @Size(max = 255, message = "subject must be <= 255 characters")
    String subject,
    @NotBlank(message = "bodyTemplate is required")
    String bodyTemplate,
    @Size(max = 32, message = "channel must be <= 32 characters")
    String channel,
    @NotNull(message = "enabled is required") Boolean enabled) {}
