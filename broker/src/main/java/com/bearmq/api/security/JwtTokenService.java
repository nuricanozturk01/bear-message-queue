package com.bearmq.api.security;

import com.bearmq.api.tenant.dto.TenantAuthenticateInfo;
import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import com.bearmq.shared.tenant.dto.TenantInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private static final String CLAIM_TYPE = "typ";
  private static final String CLAIM_USERNAME = "username";
  private static final String CLAIM_STATUS = "sts";
  private static final String CLAIM_ROLE = "role";
  private static final String TYPE_ACCESS = "access";
  private static final String TYPE_REFRESH = "refresh";

  private final SecretKey secretKey;
  private final Duration accessTtl;
  private final Duration refreshTtl;

  public JwtTokenService(final JwtProperties props) {
    final byte[] keyBytes = props.secret().getBytes(StandardCharsets.UTF_8);
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    this.accessTtl = Duration.ofMinutes(props.accessTokenExpirationMinutes());
    this.refreshTtl = Duration.ofDays(props.refreshTokenExpirationDays());
  }

  public String createAccessToken(final TenantAuthenticateInfo tenant) {
    final Instant now = Instant.now();
    return Jwts.builder()
        .subject(tenant.id())
        .claim(CLAIM_TYPE, TYPE_ACCESS)
        .claim(CLAIM_USERNAME, tenant.username())
        .claim(CLAIM_STATUS, tenant.status().name())
        .claim(CLAIM_ROLE, tenant.role() != null ? tenant.role().name() : TenantRole.USER.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(this.accessTtl)))
        .signWith(this.secretKey)
        .compact();
  }

  public String createRefreshToken(final String tenantId) {
    final Instant now = Instant.now();
    return Jwts.builder()
        .subject(tenantId)
        .claim(CLAIM_TYPE, TYPE_REFRESH)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(this.refreshTtl)))
        .signWith(this.secretKey)
        .compact();
  }

  public TenantInfo parseAccessToken(final String token) {
    final Claims claims = this.parseClaims(token);
    assertTokenType(claims, TYPE_ACCESS);
    return toTenantInfo(claims);
  }

  public String parseRefreshToken(final String token) {
    final Claims claims = this.parseClaims(token);
    assertTokenType(claims, TYPE_REFRESH);
    return claims.getSubject();
  }

  public boolean isLikelyJwt(final String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    return token.chars().filter(ch -> ch == '.').count() == 2;
  }

  private Claims parseClaims(final String token) {
    try {
      return Jwts.parser().verifyWith(this.secretKey).build().parseSignedClaims(token).getPayload();
    } catch (final ExpiredJwtException ex) {
      throw new JwtException("Token expired", ex);
    }
  }

  private static void assertTokenType(final Claims claims, final String expected) {
    final Object typ = claims.get(CLAIM_TYPE);
    if (typ == null || !expected.equals(typ.toString())) {
      throw new JwtException("Invalid token type");
    }
  }

  private static TenantInfo toTenantInfo(final Claims claims) {
    final String roleClaim = claims.get(CLAIM_ROLE, String.class);
    final TenantRole role = roleClaim != null ? TenantRole.valueOf(roleClaim) : TenantRole.USER;
    return new TenantInfo(
        claims.getSubject(),
        claims.get(CLAIM_USERNAME, String.class),
        TenantStatus.valueOf(claims.get(CLAIM_STATUS, String.class)),
        role);
  }
}
