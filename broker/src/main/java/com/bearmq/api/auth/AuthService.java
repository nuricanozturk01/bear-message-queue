package com.bearmq.api.auth;

import com.bearmq.api.auth.dto.AuthRequest;
import com.bearmq.api.auth.dto.AuthResponse;
import com.bearmq.api.auth.dto.RegisterRequest;
import com.bearmq.api.common.exception.UnauthorizedException;
import com.bearmq.api.security.JwtTokenService;
import com.bearmq.api.tenant.TenantService;
import com.bearmq.api.tenant.dto.TenantAuthenticateInfo;
import com.bearmq.shared.settings.MessagingApiKeyService;
import com.bearmq.shared.tenant.TenantStatus;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final TenantService tenantService;
  private final JwtTokenService jwtTokenService;
  private final MessagingApiKeyService messagingApiKeyService;

  public AuthResponse register(final RegisterRequest request) {
    final TenantAuthenticateInfo info = this.tenantService.create(request);
    return this.toAuthResponse(info);
  }

  public AuthResponse login(final AuthRequest request) {
    final TenantAuthenticateInfo user =
        this.tenantService.requireAuthenticateByUsername(request.username());
    if (user.status() == TenantStatus.DELETED || user.status() == TenantStatus.SUSPENDED) {
      throw new UnauthorizedException("Account is not active");
    }
    final String hashed = DigestUtils.sha256Hex(user.salt() + request.password());
    if (!user.password().equals(hashed)) {
      throw new UnauthorizedException("Invalid credentials");
    }
    return this.toAuthResponse(user);
  }

  public AuthResponse refresh(final String refreshToken) {
    final String tenantId;
    try {
      tenantId = this.jwtTokenService.parseRefreshToken(refreshToken);
    } catch (final JwtException ex) {
      throw new UnauthorizedException("Invalid refresh token");
    }
    final TenantAuthenticateInfo user = this.tenantService.getAuthenticateInfoById(tenantId);
    if (user.status() == TenantStatus.DELETED || user.status() == TenantStatus.SUSPENDED) {
      throw new UnauthorizedException("Account is not active");
    }
    return this.toAuthResponse(user);
  }

  private AuthResponse toAuthResponse(final TenantAuthenticateInfo info) {
    return AuthResponse.builder()
        .token(this.jwtTokenService.createAccessToken(info))
        .refreshToken(this.jwtTokenService.createRefreshToken(info.id()))
        .apiKey(this.messagingApiKeyService.getMessagingApiKey())
        .build();
  }
}
