package com.eegalepoint.citybus.passenger.dto;

import java.time.Instant;
import java.time.LocalTime;

public record ReservationResponse(
    long id,
    long scheduleId,
    String tripCode,
    LocalTime departureTime,
    long stopId,
    String stopCode,
    String stopName,
    String status,
    Instant reservedAt,
    Instant updatedAt) {}
