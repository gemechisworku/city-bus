package com.eegalepoint.citybus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthHandlers {

  private final ObjectMapper objectMapper;

  public JsonAuthHandlers(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public AuthenticationEntryPoint authenticationEntryPoint() {
    return (HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) ->
        writeJson(
            response,
            HttpServletResponse.SC_UNAUTHORIZED,
            Map.of("error", "UNAUTHORIZED", "message", "Authentication required"));
  }

  public AccessDeniedHandler accessDeniedHandler() {
    return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) ->
        writeJson(
            response,
            HttpServletResponse.SC_FORBIDDEN,
            Map.of("error", "FORBIDDEN", "message", "Insufficient privileges"));
  }

  private void writeJson(HttpServletResponse response, int status, Map<String, String> body)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
