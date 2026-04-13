package com.eegalepoint.citybus.workflow.dto;

import java.util.List;

public record BatchResultResponse(int processed, List<TaskResponse> tasks) {}
