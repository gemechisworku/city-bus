package com.eegalepoint.citybus.transit;

import com.eegalepoint.citybus.domain.transit.StopEntity;
import com.eegalepoint.citybus.domain.transit.StopRepository;
import com.eegalepoint.citybus.domain.transit.StopVersionEntity;
import com.eegalepoint.citybus.domain.transit.StopVersionRepository;
import com.eegalepoint.citybus.transit.dto.StopDetailResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StopQueryService {

  private final StopRepository stopRepository;
  private final StopVersionRepository stopVersionRepository;

  public StopQueryService(StopRepository stopRepository, StopVersionRepository stopVersionRepository) {
    this.stopRepository = stopRepository;
    this.stopVersionRepository = stopVersionRepository;
  }

  @Transactional(readOnly = true)
  public StopDetailResponse getStop(long stopId) {
    StopEntity stop =
        stopRepository
            .findById(stopId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stop not found"));
    StopVersionEntity sv =
        stopVersionRepository
            .findFirstByStop_IdOrderByVersionNumberDesc(stop.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No stop versions"));
    return new StopDetailResponse(
        stop.getId(),
        stop.getCode(),
        sv.getVersionNumber(),
        sv.getName(),
        sv.getLatitude(),
        sv.getLongitude(),
        sv.getEffectiveFrom());
  }
}
