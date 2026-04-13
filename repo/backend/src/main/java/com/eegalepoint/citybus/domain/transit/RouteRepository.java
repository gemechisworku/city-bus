package com.eegalepoint.citybus.domain.transit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<RouteEntity, Long> {

  Optional<RouteEntity> findByCode(String code);
}
