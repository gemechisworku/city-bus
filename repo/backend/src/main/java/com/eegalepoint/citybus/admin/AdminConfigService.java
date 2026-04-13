package com.eegalepoint.citybus.admin;

import com.eegalepoint.citybus.admin.dto.AuditLogResponse;
import com.eegalepoint.citybus.admin.dto.CleaningRuleResponse;
import com.eegalepoint.citybus.admin.dto.DictionaryEntryResponse;
import com.eegalepoint.citybus.admin.dto.FieldMappingResponse;
import com.eegalepoint.citybus.admin.dto.NotificationTemplateResponse;
import com.eegalepoint.citybus.admin.dto.RankingConfigResponse;
import com.eegalepoint.citybus.admin.dto.SaveCleaningRuleRequest;
import com.eegalepoint.citybus.admin.dto.SaveDictionaryEntryRequest;
import com.eegalepoint.citybus.admin.dto.SaveNotificationTemplateRequest;
import com.eegalepoint.citybus.admin.dto.UpdateRankingConfigRequest;
import com.eegalepoint.citybus.admin.dto.UpdateUserRequest;
import com.eegalepoint.citybus.admin.dto.UserAdminResponse;
import com.eegalepoint.citybus.domain.RoleEntity;
import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.domain.config.CleaningRuleSetEntity;
import com.eegalepoint.citybus.domain.config.CleaningRuleSetRepository;
import com.eegalepoint.citybus.domain.config.FieldStandardDictionaryEntity;
import com.eegalepoint.citybus.domain.config.FieldStandardDictionaryRepository;
import com.eegalepoint.citybus.domain.config.NotificationTemplateEntity;
import com.eegalepoint.citybus.domain.config.NotificationTemplateRepository;
import com.eegalepoint.citybus.domain.search.RankingConfigEntity;
import com.eegalepoint.citybus.domain.search.RankingConfigRepository;
import com.eegalepoint.citybus.domain.transit.FieldMappingEntity;
import com.eegalepoint.citybus.domain.transit.FieldMappingRepository;
import com.eegalepoint.citybus.repo.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminConfigService {

  private final CleaningRuleSetRepository cleaningRuleRepository;
  private final FieldStandardDictionaryRepository dictionaryRepository;
  private final RankingConfigRepository rankingConfigRepository;
  private final FieldMappingRepository fieldMappingRepository;
  private final NotificationTemplateRepository notificationTemplateRepository;
  private final UserRepository userRepository;
  private final JdbcTemplate jdbcTemplate;

  public AdminConfigService(
      CleaningRuleSetRepository cleaningRuleRepository,
      FieldStandardDictionaryRepository dictionaryRepository,
      RankingConfigRepository rankingConfigRepository,
      FieldMappingRepository fieldMappingRepository,
      NotificationTemplateRepository notificationTemplateRepository,
      UserRepository userRepository,
      JdbcTemplate jdbcTemplate) {
    this.cleaningRuleRepository = cleaningRuleRepository;
    this.dictionaryRepository = dictionaryRepository;
    this.rankingConfigRepository = rankingConfigRepository;
    this.fieldMappingRepository = fieldMappingRepository;
    this.notificationTemplateRepository = notificationTemplateRepository;
    this.userRepository = userRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  // ── Cleaning rules ──

  @Transactional(readOnly = true)
  public List<CleaningRuleResponse> listCleaningRules() {
    return cleaningRuleRepository.findAll().stream().map(this::toCleaningResponse).toList();
  }

  @Transactional
  public CleaningRuleResponse createCleaningRule(SaveCleaningRuleRequest req) {
    CleaningRuleSetEntity entity = new CleaningRuleSetEntity();
    applyCleaningFields(entity, req);
    return toCleaningResponse(cleaningRuleRepository.save(entity));
  }

  @Transactional
  public CleaningRuleResponse updateCleaningRule(long id, SaveCleaningRuleRequest req) {
    CleaningRuleSetEntity entity = cleaningRuleRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cleaning rule not found"));
    applyCleaningFields(entity, req);
    return toCleaningResponse(cleaningRuleRepository.save(entity));
  }

  @Transactional
  public void deleteCleaningRule(long id) {
    if (!cleaningRuleRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cleaning rule not found");
    }
    cleaningRuleRepository.deleteById(id);
  }

  private void applyCleaningFields(CleaningRuleSetEntity entity, SaveCleaningRuleRequest req) {
    entity.setName(req.name());
    entity.setDescription(req.description());
    entity.setFieldTarget(req.fieldTarget());
    entity.setPattern(req.pattern());
    entity.setReplacement(req.replacement() != null ? req.replacement() : "");
    entity.setEnabled(req.enabled());
  }

  private CleaningRuleResponse toCleaningResponse(CleaningRuleSetEntity e) {
    return new CleaningRuleResponse(
        e.getId(), e.getName(), e.getDescription(), e.getFieldTarget(),
        e.getRuleType(), e.getPattern(), e.getReplacement(), e.isEnabled(),
        e.getCreatedAt(), e.getUpdatedAt());
  }

  // ── Dictionaries ──

  @Transactional(readOnly = true)
  public List<DictionaryEntryResponse> listDictionaries() {
    return dictionaryRepository.findAllByOrderByFieldNameAscCanonicalValueAsc()
        .stream().map(this::toDictionaryResponse).toList();
  }

  @Transactional
  public DictionaryEntryResponse createDictionary(SaveDictionaryEntryRequest req) {
    FieldStandardDictionaryEntity entity = new FieldStandardDictionaryEntity();
    applyDictionaryFields(entity, req);
    return toDictionaryResponse(dictionaryRepository.save(entity));
  }

  @Transactional
  public DictionaryEntryResponse updateDictionary(long id, SaveDictionaryEntryRequest req) {
    FieldStandardDictionaryEntity entity = dictionaryRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dictionary entry not found"));
    applyDictionaryFields(entity, req);
    return toDictionaryResponse(dictionaryRepository.save(entity));
  }

  @Transactional
  public void deleteDictionary(long id) {
    if (!dictionaryRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dictionary entry not found");
    }
    dictionaryRepository.deleteById(id);
  }

  private void applyDictionaryFields(FieldStandardDictionaryEntity entity, SaveDictionaryEntryRequest req) {
    entity.setFieldName(req.fieldName());
    entity.setCanonicalValue(req.canonicalValue());
    entity.setAliases(req.aliases());
    entity.setEnabled(req.enabled());
  }

  private DictionaryEntryResponse toDictionaryResponse(FieldStandardDictionaryEntity e) {
    return new DictionaryEntryResponse(
        e.getId(), e.getFieldName(), e.getCanonicalValue(),
        e.getAliases(), e.isEnabled(), e.getCreatedAt(), e.getUpdatedAt());
  }

  // ── Ranking config ──

  @Transactional(readOnly = true)
  public RankingConfigResponse getRankingConfig() {
    RankingConfigEntity cfg = rankingConfigRepository.findByConfigKey("DEFAULT")
        .orElseThrow(() -> new IllegalStateException("Missing DEFAULT ranking_config row"));
    return toRankingResponse(cfg);
  }

  @Transactional
  public RankingConfigResponse updateRankingConfig(UpdateRankingConfigRequest req) {
    RankingConfigEntity cfg = rankingConfigRepository.findByConfigKey("DEFAULT")
        .orElseThrow(() -> new IllegalStateException("Missing DEFAULT ranking_config row"));
    cfg.setRouteWeight(req.routeWeight());
    cfg.setStopWeight(req.stopWeight());
    cfg.setPopularityWeight(req.popularityWeight());
    cfg.setMaxSuggestions(req.maxSuggestions());
    cfg.setMaxResults(req.maxResults());
    return toRankingResponse(rankingConfigRepository.save(cfg));
  }

  private RankingConfigResponse toRankingResponse(RankingConfigEntity e) {
    return new RankingConfigResponse(
        e.getId(), e.getConfigKey(), e.getRouteWeight(), e.getStopWeight(),
        e.getPopularityWeight(), e.getMaxSuggestions(), e.getMaxResults(), e.getUpdatedAt());
  }

  // ── Templates (field mappings) ──

  @Transactional(readOnly = true)
  public List<FieldMappingResponse> listTemplates() {
    return fieldMappingRepository.findAll().stream()
        .map(e -> new FieldMappingResponse(e.getId(), e.getTemplateName(), e.getSourceField(), e.getTargetField(), e.getCreatedAt()))
        .toList();
  }

  // ── Notification templates (messages) ──

  @Transactional(readOnly = true)
  public List<NotificationTemplateResponse> listNotificationTemplates() {
    return notificationTemplateRepository.findAllByOrderByCodeAsc().stream()
        .map(this::toNotificationTemplateResponse)
        .toList();
  }

  @Transactional
  public NotificationTemplateResponse createNotificationTemplate(SaveNotificationTemplateRequest req) {
    if (notificationTemplateRepository.existsByCode(req.code())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Notification template code already exists");
    }
    NotificationTemplateEntity entity = new NotificationTemplateEntity();
    applyNotificationTemplateFields(entity, req);
    return toNotificationTemplateResponse(notificationTemplateRepository.save(entity));
  }

  @Transactional
  public NotificationTemplateResponse updateNotificationTemplate(long id, SaveNotificationTemplateRequest req) {
    NotificationTemplateEntity entity = notificationTemplateRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification template not found"));
    if (!entity.getCode().equals(req.code()) && notificationTemplateRepository.existsByCode(req.code())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Notification template code already exists");
    }
    applyNotificationTemplateFields(entity, req);
    return toNotificationTemplateResponse(notificationTemplateRepository.save(entity));
  }

  @Transactional
  public void deleteNotificationTemplate(long id) {
    if (!notificationTemplateRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification template not found");
    }
    notificationTemplateRepository.deleteById(id);
  }

  private void applyNotificationTemplateFields(NotificationTemplateEntity entity, SaveNotificationTemplateRequest req) {
    entity.setCode(req.code());
    entity.setSubject(req.subject());
    entity.setBodyTemplate(req.bodyTemplate());
    entity.setChannel(req.channel() != null && !req.channel().isBlank() ? req.channel() : "IN_APP");
    entity.setEnabled(req.enabled());
  }

  private NotificationTemplateResponse toNotificationTemplateResponse(NotificationTemplateEntity e) {
    return new NotificationTemplateResponse(
        e.getId(), e.getCode(), e.getSubject(), e.getBodyTemplate(),
        e.getChannel(), e.isEnabled(), e.getCreatedAt(), e.getUpdatedAt());
  }

  // ── Users ──

  @Transactional(readOnly = true)
  public List<UserAdminResponse> listUsers() {
    return userRepository.findAll().stream().map(this::toUserResponse).toList();
  }

  @Transactional(readOnly = true)
  public UserAdminResponse getUser(long id) {
    UserEntity user = userRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return toUserResponse(user);
  }

  @Transactional
  public UserAdminResponse updateUser(long id, UpdateUserRequest req) {
    UserEntity user = userRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    user.setEnabled(req.enabled());
    return toUserResponse(userRepository.save(user));
  }

  private UserAdminResponse toUserResponse(UserEntity u) {
    List<String> roles = u.getRoles().stream().map(RoleEntity::getName).sorted().toList();
    return new UserAdminResponse(u.getId(), u.getUsername(), u.isEnabled(), roles, u.getCreatedAt());
  }

  // ── Audit log ──

  @Transactional(readOnly = true)
  public List<AuditLogResponse> listAuditLogs() {
    return jdbcTemplate.query(
        "SELECT id, user_id, username_attempt, success, ip_address, created_at FROM login_audit ORDER BY created_at DESC LIMIT 200",
        (rs, i) -> new AuditLogResponse(
            rs.getLong("id"),
            rs.getObject("user_id") != null ? rs.getLong("user_id") : null,
            rs.getString("username_attempt"),
            rs.getBoolean("success"),
            rs.getString("ip_address"),
            rs.getTimestamp("created_at").toInstant()));
  }
}
