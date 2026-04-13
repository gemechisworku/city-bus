package com.eegalepoint.citybus.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RankingConfigResponse(
    long id,
    String configKey,
    BigDecimal routeWeight,
    BigDecimal stopWeight,
    BigDecimal popularityWeight,
    int maxSuggestions,
    int maxResults,
    Instant updatedAt) {}
