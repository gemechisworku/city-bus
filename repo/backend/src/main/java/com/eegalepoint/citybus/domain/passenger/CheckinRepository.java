package com.eegalepoint.citybus.domain.passenger;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckinRepository extends JpaRepository<CheckinEntity, Long> {

  List<CheckinEntity> findByUser_IdOrderByCheckedInAtDesc(Long userId);
}
