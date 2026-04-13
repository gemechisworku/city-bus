package com.eegalepoint.citybus.ingestion;

import com.eegalepoint.citybus.domain.config.CleaningAuditLogEntity;
import com.eegalepoint.citybus.domain.config.CleaningAuditLogRepository;
import com.eegalepoint.citybus.domain.config.CleaningRuleSetEntity;
import com.eegalepoint.citybus.domain.config.CleaningRuleSetRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataCleaningService {

  private static final Logger log = LoggerFactory.getLogger(DataCleaningService.class);

  private final CleaningRuleSetRepository ruleRepository;
  private final CleaningAuditLogRepository auditLogRepository;

  public DataCleaningService(
      CleaningRuleSetRepository ruleRepository,
      CleaningAuditLogRepository auditLogRepository) {
    this.ruleRepository = ruleRepository;
    this.auditLogRepository = auditLogRepository;
  }

  /**
   * Applies all enabled cleaning rules matching the given field target.
   * Records each transformation in the audit log.
   * Returns null for null/blank inputs (source logging as NULL).
   */
  public String clean(String fieldTarget, String value) {
    if (value == null || value.isBlank()) {
      log.debug("Null/blank value for field '{}' — recorded as NULL", fieldTarget);
      return null;
    }

    List<CleaningRuleSetEntity> rules =
        ruleRepository.findByEnabledTrueOrderByNameAsc().stream()
            .filter(r -> r.getFieldTarget().equalsIgnoreCase(fieldTarget)
                || "*".equals(r.getFieldTarget()))
            .toList();

    String cleaned = value;
    for (CleaningRuleSetEntity rule : rules) {
      try {
        String after = cleaned.replaceAll(rule.getPattern(), rule.getReplacement());
        if (!after.equals(cleaned)) {
          auditLogRepository.save(
              new CleaningAuditLogEntity(rule, cleaned, after, null));
          log.debug("Rule '{}' applied to '{}': '{}' → '{}'",
              rule.getName(), fieldTarget, cleaned, after);
          cleaned = after;
        }
      } catch (Exception ex) {
        log.warn("Cleaning rule '{}' failed on value '{}': {}",
            rule.getName(), value, ex.getMessage());
      }
    }
    return cleaned;
  }
}
