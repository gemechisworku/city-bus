package com.eegalepoint.citybus.transit.dto;

public record ImportRunResponse(long jobId, String status, Integer rowCount, String errorMessage) {}
