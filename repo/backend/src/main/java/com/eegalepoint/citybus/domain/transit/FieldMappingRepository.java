package com.eegalepoint.citybus.domain.transit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldMappingRepository extends JpaRepository<FieldMappingEntity, Long> {

  boolean existsByTemplateName(String templateName);
}
