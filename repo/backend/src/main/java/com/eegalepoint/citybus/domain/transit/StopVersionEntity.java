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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "stop_versions")
public class StopVersionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stop_id", nullable = false)
  private StopEntity stop;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Column(nullable = false)
  private String name;

  private BigDecimal latitude;

  private BigDecimal longitude;

  @Column(name = "effective_from")
  private LocalDate effectiveFrom;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected StopVersionEntity() {}

  public StopVersionEntity(
      StopEntity stop,
      int versionNumber,
      String name,
      BigDecimal latitude,
      BigDecimal longitude,
      LocalDate effectiveFrom) {
    this.stop = stop;
    this.versionNumber = versionNumber;
    this.name = name;
    this.latitude = latitude;
    this.longitude = longitude;
    this.effectiveFrom = effectiveFrom;
  }

  public Long getId() {
    return id;
  }

  public StopEntity getStop() {
    return stop;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getLatitude() {
    return latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
