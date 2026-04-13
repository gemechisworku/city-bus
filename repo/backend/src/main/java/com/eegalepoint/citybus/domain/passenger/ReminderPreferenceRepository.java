package com.eegalepoint.citybus.domain.passenger;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderPreferenceRepository extends JpaRepository<ReminderPreferenceEntity, Long> {

  Optional<ReminderPreferenceEntity> findByUser_Id(Long userId);
}
