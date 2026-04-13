package com.eegalepoint.citybus.domain.workflow;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEscalationRepository extends JpaRepository<WorkflowEscalationEntity, Long> {

  List<WorkflowEscalationEntity> findByTask_IdOrderByCreatedAtDesc(Long taskId);
}
