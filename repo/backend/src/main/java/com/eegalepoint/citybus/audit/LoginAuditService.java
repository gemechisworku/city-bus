package com.eegalepoint.citybus.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAuditService {

  private final JdbcTemplate jdbcTemplate;

  public LoginAuditService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public void record(Long userId, String usernameAttempt, boolean success, HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    if (request.getHeader("X-Forwarded-For") != null) {
      ip = request.getHeader("X-Forwarded-For").split(",")[0].trim();
    }
    String ua = request.getHeader("User-Agent");
    if (ua != null && ua.length() > 512) {
      ua = ua.substring(0, 512);
    }
    jdbcTemplate.update(
        "INSERT INTO login_audit (user_id, username_attempt, success, ip_address, user_agent) VALUES (?,?,?,?,?)",
        userId,
        usernameAttempt,
        success,
        ip,
        ua);
  }
}
