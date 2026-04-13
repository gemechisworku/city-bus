package com.eegalepoint.citybus.admin.dto;

import java.time.Instant;

public record DictionaryEntryResponse(
    long id,
    String fieldName,
    String canonicalValue,
    String aliases,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {}
