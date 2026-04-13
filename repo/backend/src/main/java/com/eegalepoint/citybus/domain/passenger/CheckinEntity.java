package com.eegalepoint.citybus.domain.passenger;

import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.domain.transit.StopEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "passenger_checkins")
public class CheckinEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reservation_id")
  private ReservationEntity reservation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stop_id", nullable = false)
  private StopEntity stop;

  @Column(name = "checked_in_at", nullable = false)
  private Instant checkedInAt = Instant.now();

  protected CheckinEntity() {}

  public CheckinEntity(UserEntity user, ReservationEntity reservation, StopEntity stop) {
    this.user = user;
    this.reservation = reservation;
    this.stop = stop;
  }

  public Long getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public ReservationEntity getReservation() {
    return reservation;
  }

  public StopEntity getStop() {
    return stop;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }
}
