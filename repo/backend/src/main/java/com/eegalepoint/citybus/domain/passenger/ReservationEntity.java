package com.eegalepoint.citybus.domain.passenger;

import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.domain.transit.ScheduleEntity;
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
@Table(name = "passenger_reservations")
public class ReservationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "schedule_id", nullable = false)
  private ScheduleEntity schedule;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stop_id", nullable = false)
  private StopEntity stop;

  @Column(nullable = false, length = 32)
  private String status = "PENDING";

  @Column(name = "reserved_at", nullable = false)
  private Instant reservedAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected ReservationEntity() {}

  public ReservationEntity(UserEntity user, ScheduleEntity schedule, StopEntity stop) {
    this.user = user;
    this.schedule = schedule;
    this.stop = stop;
  }

  public Long getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public ScheduleEntity getSchedule() {
    return schedule;
  }

  public StopEntity getStop() {
    return stop;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public Instant getReservedAt() {
    return reservedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
