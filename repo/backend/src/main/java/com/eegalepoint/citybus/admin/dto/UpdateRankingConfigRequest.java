package com.eegalepoint.citybus.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateRankingConfigRequest(
    @NotNull(message = "routeWeight is required")
    @DecimalMin(value = "0.0", message = "routeWeight must be >= 0")
    BigDecimal routeWeight,
    @NotNull(message = "stopWeight is required")
    @DecimalMin(value = "0.0", message = "stopWeight must be >= 0")
    BigDecimal stopWeight,
    @NotNull(message = "popularityWeight is required")
    @DecimalMin(value = "0.0", message = "popularityWeight must be >= 0")
    BigDecimal popularityWeight,
    @NotNull(message = "maxSuggestions is required")
    @Min(value = 1, message = "maxSuggestions must be >= 1")
    Integer maxSuggestions,
    @NotNull(message = "maxResults is required")
    @Min(value = 1, message = "maxResults must be >= 1")
    Integer maxResults) {}
