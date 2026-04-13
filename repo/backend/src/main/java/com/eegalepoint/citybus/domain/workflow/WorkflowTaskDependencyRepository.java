package com.eegalepoint.citybus.domain.workflow;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTaskDependencyRepository extends JpaRepository<WorkflowTaskDependencyEntity, Long> {

  List<WorkflowTaskDependencyEntity> findByTask_IdOrderByDependsOn_IdAsc(long taskId);

  /** Tasks that must wait until {@code dependsOnTaskId} is approved (depend on that task). */
  List<WorkflowTaskDependencyEntity> findByDependsOn_Id(long dependsOnTaskId);
}
