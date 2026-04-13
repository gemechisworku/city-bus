package com.eegalepoint.citybus.auth.jwt;

import com.eegalepoint.citybus.config.AppJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final AppJwtProperties props;
  private final SecretKey key;

  public JwtService(AppJwtProperties props) {
    this.props = props;
    byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes)");
    }
    this.key = Keys.hmacShaKeyFor(bytes);
  }

  public String createToken(String username, List<String> roleNames) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.expirationSeconds());
    return Jwts.builder()
        .subject(username)
        .claim("roles", roleNames)
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key)
        .compact();
  }

  public Claims parseAndValidate(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
