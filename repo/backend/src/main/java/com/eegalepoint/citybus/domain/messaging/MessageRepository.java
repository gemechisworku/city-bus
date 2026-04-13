package com.eegalepoint.citybus.domain.messaging;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

  List<MessageEntity> findByUser_IdOrderByCreatedAtDesc(Long userId);

  Optional<MessageEntity> findByIdAndUser_Id(Long id, Long userId);
}
