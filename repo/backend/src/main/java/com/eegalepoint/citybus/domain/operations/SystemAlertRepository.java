package com.eegalepoint.citybus.domain.operations;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemAlertRepository extends JpaRepository<SystemAlertEntity, Long> {

  List<SystemAlertEntity> findByAcknowledgedFalseOrderByCreatedAtDesc();

  List<SystemAlertEntity> findAllByOrderByCreatedAtDesc();
}
