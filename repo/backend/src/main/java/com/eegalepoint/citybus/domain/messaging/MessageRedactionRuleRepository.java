package com.eegalepoint.citybus.domain.messaging;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRedactionRuleRepository extends JpaRepository<MessageRedactionRuleEntity, Long> {

  List<MessageRedactionRuleEntity> findByEnabledTrue();
}
