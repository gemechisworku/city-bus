package com.eegalepoint.citybus.domain.transit;

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
@Table(name = "schedules")
public class ScheduleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "route_version_id", nullable = false)
  private RouteVersionEntity routeVersion;

  @Column(name = "trip_code", length = 64)
  private String tripCode;

  @Column(name = "departure_time", nullable = false)
  private LocalTime departureTime;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected ScheduleEntity() {}

  public ScheduleEntity(RouteVersionEntity routeVersion, String tripCode, LocalTime departureTime) {
    this.routeVersion = routeVersion;
    this.tripCode = tripCode;
    this.departureTime = departureTime;
  }

  public Long getId() {
    return id;
  }

  public RouteVersionEntity getRouteVersion() {
    return routeVersion;
  }

  public String getTripCode() {
    return tripCode;
  }

  public LocalTime getDepartureTime() {
    return departureTime;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
