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
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

  private final AdminConfigService adminConfigService;

  public AdminConfigController(AdminConfigService adminConfigService) {
    this.adminConfigService = adminConfigService;
  }

  // ── Templates (field mappings) ──

  @GetMapping("/templates")
  public List<FieldMappingResponse> listTemplates() {
    return adminConfigService.listTemplates();
  }

  // ── Notification templates (in-app / message copy) ──

  @GetMapping("/notification-templates")
  public List<NotificationTemplateResponse> listNotificationTemplates() {
    return adminConfigService.listNotificationTemplates();
  }

  @PostMapping(path = "/notification-templates", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public NotificationTemplateResponse createNotificationTemplate(
      @Valid @RequestBody SaveNotificationTemplateRequest req) {
    return adminConfigService.createNotificationTemplate(req);
  }

  @PutMapping(path = "/notification-templates/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public NotificationTemplateResponse updateNotificationTemplate(
      @PathVariable("id") long id, @Valid @RequestBody SaveNotificationTemplateRequest req) {
    return adminConfigService.updateNotificationTemplate(id, req);
  }

  @DeleteMapping("/notification-templates/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteNotificationTemplate(@PathVariable("id") long id) {
    adminConfigService.deleteNotificationTemplate(id);
  }

  // ── Ranking config ──

  @GetMapping("/ranking-config")
  public RankingConfigResponse getRankingConfig() {
    return adminConfigService.getRankingConfig();
  }

  @PutMapping(path = "/ranking-config", consumes = MediaType.APPLICATION_JSON_VALUE)
  public RankingConfigResponse updateRankingConfig(@Valid @RequestBody UpdateRankingConfigRequest req) {
    return adminConfigService.updateRankingConfig(req);
  }

  // ── Dictionaries ──

  @GetMapping("/dictionaries")
  public List<DictionaryEntryResponse> listDictionaries() {
    return adminConfigService.listDictionaries();
  }

  @PostMapping(path = "/dictionaries", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public DictionaryEntryResponse createDictionary(@Valid @RequestBody SaveDictionaryEntryRequest req) {
    return adminConfigService.createDictionary(req);
  }

  @PutMapping(path = "/dictionaries/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public DictionaryEntryResponse updateDictionary(
      @PathVariable("id") long id, @Valid @RequestBody SaveDictionaryEntryRequest req) {
    return adminConfigService.updateDictionary(id, req);
  }

  @DeleteMapping("/dictionaries/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDictionary(@PathVariable("id") long id) {
    adminConfigService.deleteDictionary(id);
  }

  // ── Cleaning rules ──

  @GetMapping("/cleaning-rules")
  public List<CleaningRuleResponse> listCleaningRules() {
    return adminConfigService.listCleaningRules();
  }

  @PostMapping(path = "/cleaning-rules", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public CleaningRuleResponse createCleaningRule(@Valid @RequestBody SaveCleaningRuleRequest req) {
    return adminConfigService.createCleaningRule(req);
  }

  @PutMapping(path = "/cleaning-rules/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CleaningRuleResponse updateCleaningRule(
      @PathVariable("id") long id, @Valid @RequestBody SaveCleaningRuleRequest req) {
    return adminConfigService.updateCleaningRule(id, req);
  }

  @DeleteMapping("/cleaning-rules/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCleaningRule(@PathVariable("id") long id) {
    adminConfigService.deleteCleaningRule(id);
  }

  // ── Users ──

  @GetMapping("/users")
  public List<UserAdminResponse> listUsers() {
    return adminConfigService.listUsers();
  }

  @GetMapping("/users/{id}")
  public UserAdminResponse getUser(@PathVariable("id") long id) {
    return adminConfigService.getUser(id);
  }

  @PutMapping(path = "/users/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public UserAdminResponse updateUser(
      @PathVariable("id") long id, @Valid @RequestBody UpdateUserRequest req) {
    return adminConfigService.updateUser(id, req);
  }

  // ── Audit ──

  @GetMapping("/audit")
  public List<AuditLogResponse> listAuditLogs() {
    return adminConfigService.listAuditLogs();
  }
}
