package com.eegalepoint.citybus.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(@NotNull(message = "enabled is required") Boolean enabled) {}
