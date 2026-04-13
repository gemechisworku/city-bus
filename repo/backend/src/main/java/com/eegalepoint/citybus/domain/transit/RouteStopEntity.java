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

@Entity
@Table(name = "route_stops")
public class RouteStopEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "route_version_id", nullable = false)
  private RouteVersionEntity routeVersion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stop_version_id", nullable = false)
  private StopVersionEntity stopVersion;

  @Column(name = "stop_sequence", nullable = false)
  private int stopSequence;

  protected RouteStopEntity() {}

  public RouteStopEntity(
      RouteVersionEntity routeVersion, StopVersionEntity stopVersion, int stopSequence) {
    this.routeVersion = routeVersion;
    this.stopVersion = stopVersion;
    this.stopSequence = stopSequence;
  }

  public Long getId() {
    return id;
  }

  public RouteVersionEntity getRouteVersion() {
    return routeVersion;
  }

  public StopVersionEntity getStopVersion() {
    return stopVersion;
  }

  public int getStopSequence() {
    return stopSequence;
  }
}
