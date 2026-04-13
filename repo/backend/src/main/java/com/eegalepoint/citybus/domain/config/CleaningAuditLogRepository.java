package com.eegalepoint.citybus.domain.config;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleaningAuditLogRepository extends JpaRepository<CleaningAuditLogEntity, Long> {

  List<CleaningAuditLogEntity> findAllByOrderByCreatedAtDesc();
}
