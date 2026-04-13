package com.eegalepoint.citybus.domain.config;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {

  List<NotificationTemplateEntity> findAllByOrderByCodeAsc();

  boolean existsByCode(String code);
}
