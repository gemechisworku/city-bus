package com.eegalepoint.citybus.domain.passenger;

import com.eegalepoint.citybus.domain.UserEntity;
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
import java.time.LocalTime;

@Entity
@Table(name = "do_not_disturb_windows")
public class DoNotDisturbWindowEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "day_of_week", nullable = false)
  private short dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected DoNotDisturbWindowEntity() {}

  public DoNotDisturbWindowEntity(UserEntity user, short dayOfWeek, LocalTime startTime, LocalTime endTime) {
    this.user = user;
    this.dayOfWeek = dayOfWeek;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public Long getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public short getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
