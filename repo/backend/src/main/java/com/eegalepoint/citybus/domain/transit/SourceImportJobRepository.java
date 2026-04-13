package com.eegalepoint.citybus.domain.transit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceImportJobRepository extends JpaRepository<SourceImportJobEntity, Long> {

  List<SourceImportJobEntity> findAllByOrderByCreatedAtDesc();
}
