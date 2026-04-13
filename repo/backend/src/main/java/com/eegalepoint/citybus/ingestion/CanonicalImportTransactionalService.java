package com.eegalepoint.citybus.ingestion;

import com.eegalepoint.citybus.domain.transit.RouteEntity;
import com.eegalepoint.citybus.domain.transit.RouteRepository;
import com.eegalepoint.citybus.domain.transit.RouteStopEntity;
import com.eegalepoint.citybus.domain.transit.RouteStopRepository;
import com.eegalepoint.citybus.domain.transit.RouteVersionEntity;
import com.eegalepoint.citybus.domain.transit.RouteVersionRepository;
import com.eegalepoint.citybus.domain.transit.ScheduleEntity;
import com.eegalepoint.citybus.domain.transit.ScheduleRepository;
import com.eegalepoint.citybus.domain.transit.StopEntity;
import com.eegalepoint.citybus.domain.transit.StopRepository;
import com.eegalepoint.citybus.domain.transit.StopVersionEntity;
import com.eegalepoint.citybus.domain.transit.StopVersionRepository;
import com.eegalepoint.citybus.transit.dto.CanonicalImportRequest;
import com.eegalepoint.citybus.transit.dto.RouteImportDto;
import com.eegalepoint.citybus.transit.dto.ScheduleImportDto;
import com.eegalepoint.citybus.transit.dto.StopImportDto;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanonicalImportTransactionalService {

  private final RouteRepository routeRepository;
  private final RouteVersionRepository routeVersionRepository;
  private final StopRepository stopRepository;
  private final StopVersionRepository stopVersionRepository;
  private final RouteStopRepository routeStopRepository;
  private final ScheduleRepository scheduleRepository;

  public CanonicalImportTransactionalService(
      RouteRepository routeRepository,
      RouteVersionRepository routeVersionRepository,
      StopRepository stopRepository,
      StopVersionRepository stopVersionRepository,
      RouteStopRepository routeStopRepository,
      ScheduleRepository scheduleRepository) {
    this.routeRepository = routeRepository;
    this.routeVersionRepository = routeVersionRepository;
    this.stopRepository = stopRepository;
    this.stopVersionRepository = stopVersionRepository;
    this.routeStopRepository = routeStopRepository;
    this.scheduleRepository = scheduleRepository;
  }

  @Transactional
  public int importCanonical(CanonicalImportRequest request) {
    int rows = 0;
    for (RouteImportDto routeDto : request.routes()) {
      rows += importRoute(routeDto);
    }
    return rows;
  }

  private int importRoute(RouteImportDto dto) {
    RouteEntity route =
        routeRepository
            .findByCode(dto.routeCode())
            .orElseGet(() -> routeRepository.save(new RouteEntity(dto.routeCode())));
    int nextRv = routeVersionRepository.findMaxVersionNumber(route.getId()) + 1;
    RouteVersionEntity rv =
        routeVersionRepository.save(
            new RouteVersionEntity(route, nextRv, dto.name(), dto.effectiveFrom()));
    int rows = 2;

    List<StopImportDto> ordered =
        dto.stops().stream().sorted(Comparator.comparingInt(StopImportDto::sequence)).toList();
    Set<Integer> sequences = new HashSet<>();
    for (StopImportDto stopDto : ordered) {
      if (!sequences.add(stopDto.sequence())) {
        throw new IllegalArgumentException("Duplicate stop sequence " + stopDto.sequence());
      }
    }

    for (StopImportDto stopDto : ordered) {
      StopEntity stop =
          stopRepository
              .findByCode(stopDto.stopCode())
              .orElseGet(() -> stopRepository.save(new StopEntity(stopDto.stopCode())));
      int nextSv = stopVersionRepository.findMaxVersionNumber(stop.getId()) + 1;
      StopVersionEntity sv =
          stopVersionRepository.save(
              new StopVersionEntity(
                  stop,
                  nextSv,
                  stopDto.name(),
                  stopDto.latitude(),
                  stopDto.longitude(),
                  stopDto.effectiveFrom()));
      routeStopRepository.save(new RouteStopEntity(rv, sv, stopDto.sequence()));
      rows += 3;
    }

    for (ScheduleImportDto sched : dto.schedules()) {
      scheduleRepository.save(new ScheduleEntity(rv, sched.tripCode(), sched.departureTime()));
      rows += 1;
    }
    return rows;
  }
}
