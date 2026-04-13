package com.eegalepoint.citybus.transit;

import com.eegalepoint.citybus.domain.transit.RouteEntity;
import com.eegalepoint.citybus.domain.transit.RouteRepository;
import com.eegalepoint.citybus.domain.transit.RouteStopEntity;
import com.eegalepoint.citybus.domain.transit.RouteStopRepository;
import com.eegalepoint.citybus.domain.transit.RouteVersionEntity;
import com.eegalepoint.citybus.domain.transit.RouteVersionRepository;
import com.eegalepoint.citybus.domain.transit.ScheduleEntity;
import com.eegalepoint.citybus.domain.transit.ScheduleRepository;
import com.eegalepoint.citybus.transit.dto.RouteDetailResponse;
import com.eegalepoint.citybus.transit.dto.RouteSummaryResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RouteQueryService {

  private final RouteRepository routeRepository;
  private final RouteVersionRepository routeVersionRepository;
  private final RouteStopRepository routeStopRepository;
  private final ScheduleRepository scheduleRepository;

  public RouteQueryService(
      RouteRepository routeRepository,
      RouteVersionRepository routeVersionRepository,
      RouteStopRepository routeStopRepository,
      ScheduleRepository scheduleRepository) {
    this.routeRepository = routeRepository;
    this.routeVersionRepository = routeVersionRepository;
    this.routeStopRepository = routeStopRepository;
    this.scheduleRepository = scheduleRepository;
  }

  @Transactional(readOnly = true)
  public List<RouteSummaryResponse> listRoutes() {
    return routeRepository.findAll().stream()
        .flatMap(
            r ->
                routeVersionRepository
                    .findFirstByRoute_IdOrderByVersionNumberDesc(r.getId())
                    .stream()
                    .map(
                        rv ->
                            new RouteSummaryResponse(
                                r.getId(),
                                r.getCode(),
                                rv.getName(),
                                rv.getVersionNumber(),
                                rv.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public RouteDetailResponse getRoute(long routeId) {
    RouteEntity route =
        routeRepository
            .findById(routeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));
    RouteVersionEntity rv =
        routeVersionRepository
            .findFirstByRoute_IdOrderByVersionNumberDesc(route.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No route versions"));
    List<RouteStopEntity> routeStops =
        routeStopRepository.findByRouteVersionIdOrderBySequence(rv.getId());
    List<RouteDetailResponse.StopOnRouteResponse> stops =
        routeStops.stream()
            .map(
                rs -> {
                  var sv = rs.getStopVersion();
                  var s = sv.getStop();
                  return new RouteDetailResponse.StopOnRouteResponse(
                      rs.getStopSequence(),
                      s.getCode(),
                      sv.getName(),
                      sv.getLatitude(),
                      sv.getLongitude(),
                      sv.getEffectiveFrom());
                })
            .toList();
    List<ScheduleEntity> schedEntities =
        scheduleRepository.findByRouteVersionIdOrderByDepartureTimeAsc(rv.getId());
    List<RouteDetailResponse.ScheduleResponse> schedules =
        schedEntities.stream()
            .map(s -> new RouteDetailResponse.ScheduleResponse(s.getTripCode(), s.getDepartureTime()))
            .toList();
    return new RouteDetailResponse(
        route.getId(),
        route.getCode(),
        rv.getVersionNumber(),
        rv.getName(),
        rv.getEffectiveFrom(),
        stops,
        schedules);
  }
}
