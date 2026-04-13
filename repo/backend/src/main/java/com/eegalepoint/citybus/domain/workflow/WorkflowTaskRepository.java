package com.eegalepoint.citybus.domain.workflow;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTaskEntity, Long> {

  List<WorkflowTaskEntity> findByInstance_IdOrderByCreatedAtAsc(Long instanceId);

  List<WorkflowTaskEntity> findByAssignedTo_IdAndStatusOrderByCreatedAtDesc(Long userId, String status);

  List<WorkflowTaskEntity> findByStatusOrderByCreatedAtDesc(String status);
}
