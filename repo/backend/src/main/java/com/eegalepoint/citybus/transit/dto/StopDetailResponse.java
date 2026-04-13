package com.eegalepoint.citybus.transit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StopDetailResponse(
    long stopId,
    String code,
    int versionNumber,
    String name,
    BigDecimal latitude,
    BigDecimal longitude,
    LocalDate effectiveFrom) {}
