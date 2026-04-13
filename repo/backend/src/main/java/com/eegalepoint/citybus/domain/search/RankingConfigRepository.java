package com.eegalepoint.citybus.domain.search;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingConfigRepository extends JpaRepository<RankingConfigEntity, Long> {

  Optional<RankingConfigEntity> findByConfigKey(String configKey);
}
