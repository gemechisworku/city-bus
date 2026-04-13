package com.eegalepoint.citybus.admin.dto;

import java.time.Instant;

public record FieldMappingResponse(
    long id,
    String templateName,
    String sourceField,
    String targetField,
    Instant createdAt) {}
