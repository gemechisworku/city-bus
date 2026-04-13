package com.eegalepoint.citybus.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {}
