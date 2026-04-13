package com.eegalepoint.citybus.transit.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record ScheduleImportDto(String tripCode, @NotNull LocalTime departureTime) {}
