package com.eegalepoint.citybus.domain.transit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

  @Query(
      "SELECT s FROM ScheduleEntity s WHERE s.routeVersion.id = :rvId ORDER BY s.departureTime ASC")
  List<ScheduleEntity> findByRouteVersionIdOrderByDepartureTimeAsc(@Param("rvId") long routeVersionId);
}
