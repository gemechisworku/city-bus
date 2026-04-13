package com.eegalepoint.citybus.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
    @NotBlank(message = "subject is required")
    @Size(max = 255, message = "subject must be <= 255 characters")
    String subject,
    @NotBlank(message = "body is required") String body) {}
