package com.eegalepoint.citybus.domain.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stop_popularity_metrics")
public class StopPopularityMetricEntity {

  @Id
  @Column(name = "stop_id")
  private Long stopId;

  @Column(name = "impression_count", nullable = false)
  private long impressionCount;

  @Column(name = "selection_count", nullable = false)
  private long selectionCount;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected StopPopularityMetricEntity() {}

  public Long getStopId() {
    return stopId;
  }

  public long getImpressionCount() {
    return impressionCount;
  }
}
