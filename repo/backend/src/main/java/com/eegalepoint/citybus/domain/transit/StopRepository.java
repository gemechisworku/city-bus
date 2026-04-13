package com.eegalepoint.citybus.domain.transit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<StopEntity, Long> {

  Optional<StopEntity> findByCode(String code);
}
