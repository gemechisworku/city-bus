package com.eegalepoint.citybus.transit;

import com.eegalepoint.citybus.transit.dto.RouteDetailResponse;
import com.eegalepoint.citybus.transit.dto.RouteSummaryResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/routes", produces = MediaType.APPLICATION_JSON_VALUE)
public class RouteController {

  private final RouteQueryService routeQueryService;

  public RouteController(RouteQueryService routeQueryService) {
    this.routeQueryService = routeQueryService;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public List<RouteSummaryResponse> list() {
    return routeQueryService.listRoutes();
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public RouteDetailResponse get(@PathVariable("id") long id) {
    return routeQueryService.getRoute(id);
  }
}
