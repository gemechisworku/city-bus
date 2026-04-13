package com.eegalepoint.citybus.domain.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageQueueAttemptRepository extends JpaRepository<MessageQueueAttemptEntity, Long> {}
