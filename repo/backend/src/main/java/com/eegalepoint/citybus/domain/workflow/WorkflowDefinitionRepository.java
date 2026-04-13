package com.eegalepoint.citybus.domain.workflow;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinitionEntity, Long> {

  Optional<WorkflowDefinitionEntity> findByName(String name);
}
