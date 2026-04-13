package com.eegalepoint.citybus.rbac;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
public class RoleDemoController {

  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, String> adminOnly() {
    return Map.of("scope", "admin");
  }

  @GetMapping("/dispatcher")
  @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
  public Map<String, String> dispatcher() {
    return Map.of("scope", "dispatcher");
  }

  @GetMapping("/passenger")
  @PreAuthorize("hasAnyRole('ADMIN', 'PASSENGER')")
  public Map<String, String> passenger() {
    return Map.of("scope", "passenger");
  }
}
