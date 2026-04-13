package com.eegalepoint.citybus.domain.transit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StopVersionRepository extends JpaRepository<StopVersionEntity, Long> {

  @Query(
      "SELECT COALESCE(MAX(sv.versionNumber), 0) FROM StopVersionEntity sv WHERE sv.stop.id = :stopId")
  int findMaxVersionNumber(@Param("stopId") long stopId);

  Optional<StopVersionEntity> findFirstByStop_IdOrderByVersionNumberDesc(long stopId);
}
