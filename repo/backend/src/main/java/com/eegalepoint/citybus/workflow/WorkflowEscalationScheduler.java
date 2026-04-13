package com.eegalepoint.citybus.workflow;

import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.domain.operations.SystemAlertEntity;
import com.eegalepoint.citybus.domain.operations.SystemAlertRepository;
import com.eegalepoint.citybus.domain.workflow.WorkflowEscalationEntity;
import com.eegalepoint.citybus.domain.workflow.WorkflowEscalationRepository;
import com.eegalepoint.citybus.domain.workflow.WorkflowTaskEntity;
import com.eegalepoint.citybus.repo.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowEscalationScheduler {

  private static final Logger log = LoggerFactory.getLogger(WorkflowEscalationScheduler.class);
  private static final long ESCALATION_HOURS = 24;

  private final JdbcTemplate jdbcTemplate;
  private final WorkflowEscalationRepository escalationRepository;
  private final SystemAlertRepository alertRepository;
  private final UserRepository userRepository;

  public WorkflowEscalationScheduler(
      JdbcTemplate jdbcTemplate,
      WorkflowEscalationRepository escalationRepository,
      SystemAlertRepository alertRepository,
      UserRepository userRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.escalationRepository = escalationRepository;
    this.alertRepository = alertRepository;
    this.userRepository = userRepository;
  }

  @Scheduled(fixedDelayString = "${app.escalation.check-interval-ms:300000}")
  @Transactional
  public void checkOverdueTasks() {
    Instant threshold = Instant.now().minus(ESCALATION_HOURS, ChronoUnit.HOURS);

    List<Long> overdueTaskIds = jdbcTemplate.queryForList(
        """
        SELECT wt.id FROM workflow_tasks wt
        WHERE wt.status = 'PENDING'
          AND wt.created_at < ?
          AND NOT EXISTS (
            SELECT 1 FROM workflow_escalations we WHERE we.task_id = wt.id
          )
        """,
        Long.class,
        java.sql.Timestamp.from(threshold));

    if (overdueTaskIds.isEmpty()) return;

    log.warn("Found {} overdue tasks (>{} hours pending)", overdueTaskIds.size(), ESCALATION_HOURS);

    UserEntity adminUser = userRepository.findByUsername("admin").orElse(null);

    for (Long taskId : overdueTaskIds) {
      try {
        if (adminUser != null) {
          jdbcTemplate.update(
              "INSERT INTO workflow_escalations (task_id, escalated_to, reason, created_at) VALUES (?, ?, ?, NOW())",
              taskId, adminUser.getId(),
              "Task pending for over " + ESCALATION_HOURS + " hours — automatic escalation");
        }

        alertRepository.save(new SystemAlertEntity(
            "WARN", "workflow-escalation",
            "Task #" + taskId + " overdue",
            "Task has been pending for more than " + ESCALATION_HOURS + " hours"));

        log.info("Escalated overdue task id={}", taskId);
      } catch (Exception ex) {
        log.error("Failed to escalate task id={}: {}", taskId, ex.getMessage());
      }
    }
  }
}
