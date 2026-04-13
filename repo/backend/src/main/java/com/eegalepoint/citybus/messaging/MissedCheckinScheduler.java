package com.eegalepoint.citybus.messaging;

import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.domain.messaging.MessageEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueRepository;
import com.eegalepoint.citybus.domain.messaging.MessageRepository;
import com.eegalepoint.citybus.repo.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissedCheckinScheduler {

  private static final Logger log = LoggerFactory.getLogger(MissedCheckinScheduler.class);
  private static final int MISSED_THRESHOLD_MINUTES = 5;

  private final JdbcTemplate jdbcTemplate;
  private final UserRepository userRepository;
  private final MessageRepository messageRepository;
  private final MessageQueueRepository queueRepository;

  public MissedCheckinScheduler(
      JdbcTemplate jdbcTemplate,
      UserRepository userRepository,
      MessageRepository messageRepository,
      MessageQueueRepository queueRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.userRepository = userRepository;
    this.messageRepository = messageRepository;
    this.queueRepository = queueRepository;
  }

  @Scheduled(fixedDelayString = "${app.missed-checkin.check-interval-ms:60000}")
  @Transactional
  public void detectMissedCheckins() {
    try {
      List<Map<String, Object>> missed = jdbcTemplate.queryForList(
          """
          SELECT pr.id AS reservation_id,
                 pr.user_id,
                 s.trip_code,
                 st.code AS stop_code
          FROM passenger_reservations pr
          JOIN schedules s ON s.id = pr.schedule_id
          JOIN stops st ON st.id = pr.stop_id
          WHERE pr.status IN ('PENDING', 'CONFIRMED')
            AND (CURRENT_TIME - s.departure_time) > INTERVAL '%d minutes'
            AND NOT EXISTS (
              SELECT 1 FROM passenger_checkins pc
              WHERE pc.reservation_id = pr.id
            )
            AND NOT EXISTS (
              SELECT 1 FROM messages m
              WHERE m.user_id = pr.user_id
                AND m.subject = 'Missed check-in'
                AND m.created_at > NOW() - INTERVAL '1 day'
            )
          """.formatted(MISSED_THRESHOLD_MINUTES));

      if (missed.isEmpty()) return;

      log.info("Detected {} missed check-ins", missed.size());
      for (Map<String, Object> row : missed) {
        long userId = ((Number) row.get("user_id")).longValue();
        long reservationId = ((Number) row.get("reservation_id")).longValue();
        String tripCode = (String) row.get("trip_code");
        String stopCode = (String) row.get("stop_code");

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
          log.warn("User id={} not found for missed check-in notification", userId);
          continue;
        }

        MessageEntity msg = messageRepository.save(new MessageEntity(
            userOpt.get(),
            "Missed check-in",
            "You missed check-in for reservation #" + reservationId
                + " (trip " + tripCode + " at stop " + stopCode + "). "
                + "The departure time has passed by more than "
                + MISSED_THRESHOLD_MINUTES + " minutes."));
        queueRepository.save(new MessageQueueEntity(msg));
        log.info("Sent missed check-in notification for reservation id={}", reservationId);
      }
    } catch (Exception ex) {
      log.error("Missed check-in detection failed: {}", ex.getMessage());
    }
  }
}
