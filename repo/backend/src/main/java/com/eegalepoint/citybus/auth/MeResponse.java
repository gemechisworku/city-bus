package com.eegalepoint.citybus.auth;

import java.util.List;

public record MeResponse(long userId, String username, List<String> roles) {}
