package com.eegalepoint.citybus.passenger.dto;

import java.time.Instant;
import java.time.LocalTime;

public record DndWindowResponse(
    long id,
    short dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Instant createdAt) {}
