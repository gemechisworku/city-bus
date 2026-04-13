package com.eegalepoint.citybus.admin.dto;

import java.time.Instant;
import java.util.List;

public record UserAdminResponse(
    long id,
    String username,
    boolean enabled,
    List<String> roles,
    Instant createdAt) {}
