package com.eegalepoint.citybus.domain.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageQueueRepository extends JpaRepository<MessageQueueEntity, Long> {}
