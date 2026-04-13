package com.eegalepoint.citybus.transit.dto;

public record RouteSummaryResponse(
    long id, String code, String name, int latestVersionNumber, long latestRouteVersionId) {}
