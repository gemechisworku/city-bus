package com.eegalepoint.citybus.domain.config;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleaningRuleSetRepository extends JpaRepository<CleaningRuleSetEntity, Long> {

  List<CleaningRuleSetEntity> findByEnabledTrueOrderByNameAsc();

  Optional<CleaningRuleSetEntity> findByName(String name);
}
