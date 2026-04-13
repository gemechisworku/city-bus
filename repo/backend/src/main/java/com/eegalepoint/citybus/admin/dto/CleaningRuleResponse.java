package com.eegalepoint.citybus.admin.dto;

import java.time.Instant;

public record CleaningRuleResponse(
    long id,
    String name,
    String description,
    String fieldTarget,
    String ruleType,
    String pattern,
    String replacement,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {}
