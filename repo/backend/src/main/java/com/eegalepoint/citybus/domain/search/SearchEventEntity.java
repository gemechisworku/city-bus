package com.eegalepoint.citybus.domain.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "search_events")
public class SearchEventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "query_text", nullable = false)
  private String queryText;

  @Column(nullable = false, length = 16)
  private String scope;

  @Column(name = "result_count", nullable = false)
  private int resultCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected SearchEventEntity() {}

  public SearchEventEntity(String queryText, String scope, int resultCount) {
    this.queryText = queryText;
    this.scope = scope;
    this.resultCount = resultCount;
  }
}
