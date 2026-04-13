package com.eegalepoint.citybus.passenger.dto;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
    @NotNull(message = "scheduleId is required") Long scheduleId,
    @NotNull(message = "stopId is required") Long stopId) {}
