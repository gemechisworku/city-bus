package com.eegalepoint.citybus.domain.transit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteStopRepository extends JpaRepository<RouteStopEntity, Long> {

  @Query(
      "SELECT rs FROM RouteStopEntity rs JOIN FETCH rs.stopVersion sv JOIN FETCH sv.stop WHERE rs.routeVersion.id = :rvId ORDER BY rs.stopSequence ASC")
  List<RouteStopEntity> findByRouteVersionIdOrderBySequence(@Param("rvId") long routeVersionId);
}
