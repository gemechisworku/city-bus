package com.eegalepoint.citybus.domain.passenger;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoNotDisturbWindowRepository extends JpaRepository<DoNotDisturbWindowEntity, Long> {

  List<DoNotDisturbWindowEntity> findByUser_Id(Long userId);

  void deleteByUser_Id(Long userId);
}
