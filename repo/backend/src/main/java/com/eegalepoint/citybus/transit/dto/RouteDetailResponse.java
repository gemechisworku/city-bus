package com.eegalepoint.citybus.transit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record RouteDetailResponse(
    long routeId,
    String code,
    int versionNumber,
    String name,
    LocalDate effectiveFrom,
    List<StopOnRouteResponse> stops,
    List<ScheduleResponse> schedules) {

  public record StopOnRouteResponse(
      int sequence,
      long stopId,
      String stopCode,
      String name,
      BigDecimal latitude,
      BigDecimal longitude,
      LocalDate effectiveFrom) {}

  public record ScheduleResponse(long scheduleId, String tripCode, LocalTime departureTime) {}
}
