package com.eegalepoint.citybus.passenger.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateReminderPreferenceRequest(
    @NotNull(message = "enabled is required") Boolean enabled,
    @NotNull(message = "minutesBefore is required")
    @Min(value = 1, message = "minutesBefore must be >= 1")
    @Max(value = 1440, message = "minutesBefore must be <= 1440")
    Integer minutesBefore,
    @NotBlank(message = "channel is required")
    @Pattern(regexp = "IN_APP|EMAIL|SMS", message = "channel must be IN_APP, EMAIL, or SMS")
    String channel) {}
