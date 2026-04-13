package com.eegalepoint.citybus.domain.passenger;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

  List<ReservationEntity> findByUser_IdOrderByReservedAtDesc(Long userId);
}
