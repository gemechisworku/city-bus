package com.eegalepoint.citybus.messaging;

import com.eegalepoint.citybus.domain.messaging.MessageQueueAttemptEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueAttemptRepository;
import com.eegalepoint.citybus.domain.messaging.MessageQueueEntity;
import com.eegalepoint.citybus.domain.messaging.MessageQueueRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageQueueProcessor {

  private static final Logger log = LoggerFactory.getLogger(MessageQueueProcessor.class);

  private final MessageQueueRepository queueRepository;
  private final MessageQueueAttemptRepository attemptRepository;

  public MessageQueueProcessor(
      MessageQueueRepository queueRepository,
      MessageQueueAttemptRepository attemptRepository) {
    this.queueRepository = queueRepository;
    this.attemptRepository = attemptRepository;
  }

  @Scheduled(fixedDelayString = "${app.queue.poll-interval-ms:15000}")
  @Transactional
  public void processQueue() {
    List<MessageQueueEntity> pending =
        queueRepository.findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc("QUEUED", Instant.now());
    if (pending.isEmpty()) return;

    log.info("Processing {} queued messages", pending.size());
    for (MessageQueueEntity entry : pending) {
      try {
        entry.setStatus("SENT");
        entry.setSentAt(Instant.now());
        queueRepository.save(entry);
        attemptRepository.save(new MessageQueueAttemptEntity(entry, "SUCCESS", null));
        log.debug("Delivered message queue entry id={}", entry.getId());
      } catch (Exception ex) {
        entry.setStatus("FAILED");
        queueRepository.save(entry);
        attemptRepository.save(new MessageQueueAttemptEntity(entry, "FAILURE", ex.getMessage()));
        log.warn("Failed to deliver message queue entry id={}: {}", entry.getId(), ex.getMessage());
      }
    }
  }
}
