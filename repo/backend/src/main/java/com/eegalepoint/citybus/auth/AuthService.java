package com.eegalepoint.citybus.auth;

import com.eegalepoint.citybus.audit.LoginAuditService;
import com.eegalepoint.citybus.auth.jwt.JwtService;
import com.eegalepoint.citybus.config.AppJwtProperties;
import com.eegalepoint.citybus.domain.RoleEntity;
import com.eegalepoint.citybus.domain.UserEntity;
import com.eegalepoint.citybus.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final LoginAuditService loginAuditService;
  private final AppJwtProperties jwtProperties;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      LoginAuditService loginAuditService,
      AppJwtProperties jwtProperties) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.loginAuditService = loginAuditService;
    this.jwtProperties = jwtProperties;
  }

  @Transactional
  public LoginResponse login(LoginRequest req, HttpServletRequest httpRequest) {
    var userOpt = userRepository.findByUsername(req.username().trim());
    if (userOpt.isEmpty()) {
      loginAuditService.record(null, req.username(), false, httpRequest);
      throw new BadCredentialsException("Invalid credentials");
    }
    UserEntity user = userOpt.get();
    if (!user.isEnabled()) {
      loginAuditService.record(user.getId(), req.username(), false, httpRequest);
      throw new BadCredentialsException("Invalid credentials");
    }
    if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
      loginAuditService.record(user.getId(), req.username(), false, httpRequest);
      throw new BadCredentialsException("Invalid credentials");
    }
    List<String> roleNames =
        user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList());
    String token = jwtService.createToken(user.getUsername(), roleNames);
    loginAuditService.record(user.getId(), user.getUsername(), true, httpRequest);
    return new LoginResponse(token, "Bearer", jwtProperties.expirationSeconds());
  }

  @Transactional(readOnly = true)
  public MeResponse me() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
      throw new BadCredentialsException("Not authenticated");
    }
    String username = auth.getName();
    UserEntity user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException("User not found"));
    List<String> roles =
        user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList());
    return new MeResponse(user.getId(), user.getUsername(), roles);
  }
}
