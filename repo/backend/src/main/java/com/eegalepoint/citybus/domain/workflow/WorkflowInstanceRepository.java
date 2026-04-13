package com.eegalepoint.citybus.domain.workflow;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, Long> {

  List<WorkflowInstanceEntity> findByStatusOrderByCreatedAtDesc(String status);

  List<WorkflowInstanceEntity> findAllByOrderByCreatedAtDesc();
}
