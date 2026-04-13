package com.eegalepoint.citybus.domain.operations;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosticReportRepository extends JpaRepository<DiagnosticReportEntity, Long> {

  List<DiagnosticReportEntity> findAllByOrderByStartedAtDesc();
}
