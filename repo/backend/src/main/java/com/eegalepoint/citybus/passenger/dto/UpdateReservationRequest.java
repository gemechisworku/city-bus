package com.eegalepoint.citybus.passenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateReservationRequest(
    @NotBlank(message = "status is required")
    @Pattern(regexp = "CONFIRMED|CANCELLED", message = "status must be CONFIRMED or CANCELLED")
    String status) {}
