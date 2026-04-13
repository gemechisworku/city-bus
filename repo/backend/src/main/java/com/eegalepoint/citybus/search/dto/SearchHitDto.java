package com.eegalepoint.citybus.search.dto;

public record SearchHitDto(String kind, long id, String code, String name, double score) {}
