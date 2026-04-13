package com.eegalepoint.citybus.passenger.dto;

import jakarta.validation.constraints.NotNull;

public record CreateCheckinRequest(
    @NotNull(message = "stopId is required") Long stopId,
    Long reservationId) {}
