package com.eegalepoint.citybus.domain.transit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stops")
public class StopEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String code;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected StopEntity() {}

  public StopEntity(String code) {
    this.code = code;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
