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
import java.time.LocalDate;

@Entity
@Table(name = "route_versions")
public class RouteVersionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "route_id", nullable = false)
  private RouteEntity route;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Column(nullable = false)
  private String name;

  @Column(name = "effective_from")
  private LocalDate effectiveFrom;

  @Column(name = "search_pinyin", length = 512)
  private String searchPinyin;

  @Column(name = "search_initials", length = 128)
  private String searchInitials;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected RouteVersionEntity() {}

  public RouteVersionEntity(RouteEntity route, int versionNumber, String name, LocalDate effectiveFrom) {
    this.route = route;
    this.versionNumber = versionNumber;
    this.name = name;
    this.effectiveFrom = effectiveFrom;
  }

  public Long getId() {
    return id;
  }

  public RouteEntity getRoute() {
    return route;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public String getName() {
    return name;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public String getSearchPinyin() {
    return searchPinyin;
  }

  public void setSearchPinyin(String searchPinyin) {
    this.searchPinyin = searchPinyin;
  }

  public String getSearchInitials() {
    return searchInitials;
  }

  public void setSearchInitials(String searchInitials) {
    this.searchInitials = searchInitials;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
