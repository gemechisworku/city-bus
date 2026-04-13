package com.eegalepoint.citybus.passenger.dto;

import java.time.Instant;

public record CheckinResponse(
    long id,
    long stopId,
    String stopCode,
    String stopName,
    Long reservationId,
    Instant checkedInAt) {}
