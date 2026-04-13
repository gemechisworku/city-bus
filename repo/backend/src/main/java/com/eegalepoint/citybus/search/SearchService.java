package com.eegalepoint.citybus.search;

import com.eegalepoint.citybus.domain.search.RankingConfigEntity;
import com.eegalepoint.citybus.domain.search.RankingConfigRepository;
import com.eegalepoint.citybus.domain.search.SearchEventEntity;
import com.eegalepoint.citybus.domain.search.SearchEventRepository;
import com.eegalepoint.citybus.search.dto.SearchHitDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SearchService {

  public static final String KIND_ROUTE = "ROUTE";
  public static final String KIND_STOP = "STOP";
  public static final String SCOPE_SUGGESTIONS = "SUGGESTIONS";
  public static final String SCOPE_RESULTS = "RESULTS";

  private static final Pattern QUERY_SAFE = Pattern.compile("^[a-zA-Z0-9\\s\\-]{2,128}$");

  private static final String ROUTE_SQL =
      """
      SELECT r.id, r.code, rv.name
      FROM routes r
      JOIN LATERAL (
          SELECT rv0.*
          FROM route_versions rv0
          WHERE rv0.route_id = r.id
          ORDER BY rv0.version_number DESC
          LIMIT 1
      ) rv ON TRUE
      WHERE r.code ILIKE ? OR rv.name ILIKE ?
      LIMIT 200
      """;

  private static final String STOP_SQL =
      """
      SELECT s.id, s.code, sv.name,
             COALESCE(m.impression_count, 0) AS impressions
      FROM stops s
      JOIN LATERAL (
          SELECT sv0.*
          FROM stop_versions sv0
          WHERE sv0.stop_id = s.id
          ORDER BY sv0.version_number DESC
          LIMIT 1
      ) sv ON TRUE
      LEFT JOIN stop_popularity_metrics m ON m.stop_id = s.id
      WHERE s.code ILIKE ? OR sv.name ILIKE ?
      LIMIT 200
      """;

  private final JdbcTemplate jdbcTemplate;
  private final RankingConfigRepository rankingConfigRepository;
  private final SearchEventRepository searchEventRepository;

  public SearchService(
      JdbcTemplate jdbcTemplate,
      RankingConfigRepository rankingConfigRepository,
      SearchEventRepository searchEventRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.rankingConfigRepository = rankingConfigRepository;
    this.searchEventRepository = searchEventRepository;
  }

  @Transactional
  public List<SearchHitDto> suggestions(String q, Integer limit) {
    validateQuery(q);
    String core = q.trim();
    RankingConfigEntity cfg = loadConfig();
    int max = limit != null ? Math.min(limit, 50) : cfg.getMaxSuggestions();
    max = Math.max(1, max);
    String pattern = likePattern(core);
    List<SearchHitDto> combined = rankCombined(cfg, pattern, core, max, false);
    searchEventRepository.save(new SearchEventEntity(core, SCOPE_SUGGESTIONS, combined.size()));
    return combined;
  }

  @Transactional
  public List<SearchHitDto> results(String q, Integer limit) {
    validateQuery(q);
    String core = q.trim();
    RankingConfigEntity cfg = loadConfig();
    int max = limit != null ? Math.min(limit, 100) : cfg.getMaxResults();
    max = Math.max(1, max);
    String pattern = likePattern(core);
    List<SearchHitDto> ranked = rankCombined(cfg, pattern, core, max, true);
    for (SearchHitDto hit : ranked) {
      if (KIND_STOP.equals(hit.kind())) {
        bumpStopImpression(hit.id());
      }
    }
    searchEventRepository.save(new SearchEventEntity(core, SCOPE_RESULTS, ranked.size()));
    return ranked;
  }

  private void bumpStopImpression(long stopId) {
    jdbcTemplate.update(
        """
        INSERT INTO stop_popularity_metrics (stop_id, impression_count, selection_count, updated_at)
        VALUES (?, 1, 0, NOW())
        ON CONFLICT (stop_id) DO UPDATE SET
          impression_count = stop_popularity_metrics.impression_count + 1,
          updated_at = NOW()
        """,
        stopId);
  }

  private List<SearchHitDto> rankCombined(
      RankingConfigEntity cfg,
      String likePattern,
      String queryCore,
      int max,
      boolean includePopularityForStops) {
    List<SearchHitDto> hits = new ArrayList<>();
    BigDecimal rw = cfg.getRouteWeight();
    BigDecimal sw = cfg.getStopWeight();
    BigDecimal pw = cfg.getPopularityWeight();

    for (var row : jdbcTemplate.queryForList(ROUTE_SQL, likePattern, likePattern)) {
      long id = ((Number) row.get("id")).longValue();
      String code = (String) row.get("code");
      String name = (String) row.get("name");
      double text = textMatchScore(code, name, queryCore);
      double score = text * rw.doubleValue();
      hits.add(new SearchHitDto(KIND_ROUTE, id, code, name, roundScore(score)));
    }

    for (var row : jdbcTemplate.queryForList(STOP_SQL, likePattern, likePattern)) {
      long id = ((Number) row.get("id")).longValue();
      String code = (String) row.get("code");
      String name = (String) row.get("name");
      long impressions = ((Number) row.get("impressions")).longValue();
      double text = textMatchScore(code, name, queryCore);
      double pop =
          includePopularityForStops ? pw.doubleValue() * Math.log1p(impressions) : 0d;
      double score = text * sw.doubleValue() + pop;
      hits.add(new SearchHitDto(KIND_STOP, id, code, name, roundScore(score)));
    }

    hits.sort(Comparator.comparingDouble(SearchHitDto::score).reversed());
    if (hits.size() > max) {
      return new ArrayList<>(hits.subList(0, max));
    }
    return hits;
  }

  private static double roundScore(double s) {
    return Math.round(s * 1000d) / 1000d;
  }

  /**
   * ILIKE pattern: user query is validated so it cannot inject % or _ wildcards; we wrap with %.
   */
  private static String likePattern(String q) {
    return "%" + q.trim() + "%";
  }

  private static void validateQuery(String q) {
    if (q == null || !QUERY_SAFE.matcher(q.trim()).matches()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Query must be 2–128 characters: letters, digits, spaces, and hyphen only");
    }
  }

  private RankingConfigEntity loadConfig() {
    return rankingConfigRepository
        .findByConfigKey("DEFAULT")
        .orElseThrow(() -> new IllegalStateException("Missing DEFAULT ranking_config row"));
  }

  static double textMatchScore(String code, String name, String queryCoreLower) {
    String q = queryCoreLower.toLowerCase();
    String cl = code.toLowerCase();
    String nl = name.toLowerCase();
    if (cl.equals(q)) {
      return 100d;
    }
    if (nl.equals(q)) {
      return 95d;
    }
    if (nl.startsWith(q)) {
      return 70d;
    }
    if (cl.startsWith(q)) {
      return 65d;
    }
    if (nl.contains(q)) {
      return 45d;
    }
    if (cl.contains(q)) {
      return 40d;
    }
    return 20d;
  }
}
