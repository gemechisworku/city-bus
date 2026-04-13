package com.eegalepoint.citybus.transit;

import com.eegalepoint.citybus.transit.dto.StopDetailResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/stops", produces = MediaType.APPLICATION_JSON_VALUE)
public class StopController {

  private final StopQueryService stopQueryService;

  public StopController(StopQueryService stopQueryService) {
    this.stopQueryService = stopQueryService;
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public StopDetailResponse get(@PathVariable("id") long id) {
    return stopQueryService.getStop(id);
  }
}
