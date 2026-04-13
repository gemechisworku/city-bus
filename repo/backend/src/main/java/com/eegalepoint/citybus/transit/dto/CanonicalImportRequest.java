package com.eegalepoint.citybus.transit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CanonicalImportRequest(
    @NotBlank String templateName, @NotEmpty @Valid List<RouteImportDto> routes) {}
