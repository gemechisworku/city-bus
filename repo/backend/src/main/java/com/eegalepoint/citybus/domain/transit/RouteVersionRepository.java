package com.eegalepoint.citybus.domain.transit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteVersionRepository extends JpaRepository<RouteVersionEntity, Long> {

  @Query(
      "SELECT COALESCE(MAX(rv.versionNumber), 0) FROM RouteVersionEntity rv WHERE rv.route.id = :routeId")
  int findMaxVersionNumber(@Param("routeId") long routeId);

  Optional<RouteVersionEntity> findFirstByRoute_IdOrderByVersionNumberDesc(long routeId);
}
