package com.eegalepoint.citybus.transit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

public record RouteImportDto(
    @NotBlank String routeCode,
    @NotBlank String name,
    LocalDate effectiveFrom,
    @NotEmpty @Valid List<StopImportDto> stops,
    @Valid List<ScheduleImportDto> schedules) {

  public RouteImportDto {
    schedules = schedules == null ? List.of() : schedules;
  }
}
