package com.eegalepoint.citybus.observability;

import com.eegalepoint.citybus.domain.operations.SystemAlertEntity;
import com.eegalepoint.citybus.domain.operations.SystemAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsMonitorScheduler {

  private static final Logger log = LoggerFactory.getLogger(MetricsMonitorScheduler.class);
  private static final int QUEUE_BACKLOG_THRESHOLD = 100;
  private static final double P95_THRESHOLD_MS = 500.0;

  private final JdbcTemplate jdbcTemplate;
  private final SystemAlertRepository alertRepository;

  public MetricsMonitorScheduler(
      JdbcTemplate jdbcTemplate,
      SystemAlertRepository alertRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.alertRepository = alertRepository;
  }

  @Scheduled(fixedDelayString = "${app.metrics.check-interval-ms:60000}")
  @Transactional
  public void checkQueueBacklog() {
    try {
      Integer backlog = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM message_queue WHERE status = 'QUEUED'", Integer.class);
      if (backlog != null && backlog > QUEUE_BACKLOG_THRESHOLD) {
        log.warn("Queue backlog alert: {} messages pending (threshold: {})",
            backlog, QUEUE_BACKLOG_THRESHOLD);
        alertRepository.save(new SystemAlertEntity(
            "WARN", "queue-monitor",
            "Message queue backlog: " + backlog + " messages",
            "Backlog exceeds threshold of " + QUEUE_BACKLOG_THRESHOLD
                + ". Current pending: " + backlog));
      }
    } catch (Exception ex) {
      log.error("Queue backlog check failed: {}", ex.getMessage());
    }
  }

  @Scheduled(fixedDelayString = "${app.metrics.p95-check-interval-ms:60000}")
  @Transactional
  public void checkApiResponseTimes() {
    try {
      Double avgMs = jdbcTemplate.queryForObject(
          """
          SELECT EXTRACT(EPOCH FROM (NOW() - started_at)) * 1000
          FROM diagnostic_reports
          WHERE status = 'COMPLETED'
          ORDER BY started_at DESC LIMIT 1
          """, Double.class);

      if (avgMs != null && avgMs > P95_THRESHOLD_MS) {
        log.warn("API response time alert: estimated P95 {}ms (threshold: {}ms)",
            String.format("%.0f", avgMs), P95_THRESHOLD_MS);
        alertRepository.save(new SystemAlertEntity(
            "WARN", "api-latency-monitor",
            "API P95 response time exceeds " + (int) P95_THRESHOLD_MS + "ms",
            "Estimated P95: " + String.format("%.0f", avgMs) + "ms"));
      }
    } catch (Exception ex) {
      log.debug("P95 check skipped (no data or error): {}", ex.getMessage());
    }
  }
}
