package com.eegalepoint.citybus.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveDictionaryEntryRequest(
    @NotBlank(message = "fieldName is required")
    @Size(max = 128, message = "fieldName must be <= 128 characters")
    String fieldName,
    @NotBlank(message = "canonicalValue is required")
    @Size(max = 255, message = "canonicalValue must be <= 255 characters")
    String canonicalValue,
    String aliases,
    @NotNull(message = "enabled is required") Boolean enabled) {}
