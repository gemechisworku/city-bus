package com.eegalepoint.citybus.domain.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ranking_config")
public class RankingConfigEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "config_key", nullable = false, unique = true, length = 64)
  private String configKey;

  @Column(name = "route_weight", nullable = false)
  private BigDecimal routeWeight;

  @Column(name = "stop_weight", nullable = false)
  private BigDecimal stopWeight;

  @Column(name = "popularity_weight", nullable = false)
  private BigDecimal popularityWeight;

  @Column(name = "max_suggestions", nullable = false)
  private int maxSuggestions;

  @Column(name = "max_results", nullable = false)
  private int maxResults;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected RankingConfigEntity() {}

  public BigDecimal getRouteWeight() {
    return routeWeight;
  }

  public BigDecimal getStopWeight() {
    return stopWeight;
  }

  public BigDecimal getPopularityWeight() {
    return popularityWeight;
  }

  public int getMaxSuggestions() {
    return maxSuggestions;
  }

  public int getMaxResults() {
    return maxResults;
  }
}
