package com.eegalepoint.citybus.domain.messaging;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageQueueRepository extends JpaRepository<MessageQueueEntity, Long> {

  List<MessageQueueEntity> findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
      String status, Instant before);
}
