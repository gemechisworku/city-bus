package com.eegalepoint.citybus.passenger.dto;

public record ReminderPreferenceResponse(
    boolean enabled,
    int minutesBefore,
    String channel) {}
