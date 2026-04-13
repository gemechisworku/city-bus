package com.eegalepoint.citybus.transit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StopImportDto(
    @NotBlank String stopCode,
    @NotBlank String name,
    BigDecimal latitude,
    BigDecimal longitude,
    @NotNull @Min(1) Integer sequence,
    LocalDate effectiveFrom) {}
