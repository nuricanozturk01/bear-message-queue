package com.bearmq.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bearmq.api.auth.dto.AuthRequest;
import com.bearmq.api.auth.dto.AuthResponse;
import com.bearmq.api.auth.dto.RegisterRequest;
import com.bearmq.api.common.exception.UnauthorizedException;
import com.bearmq.api.security.JwtTokenService;
import com.bearmq.api.tenant.TenantService;
import com.bearmq.api.tenant.dto.TenantAuthenticateInfo;
import com.bearmq.shared.settings.MessagingApiKeyService;
import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private TenantService tenantService;

  @Mock private JwtTokenService jwtTokenService;

  @Mock private MessagingApiKeyService messagingApiKeyService;

  @InjectMocks private AuthService authService;

  private static TenantAuthenticateInfo activeUser() {
    final String salt = "testsalt1234abcd";
    final String password = DigestUtils.sha256Hex(salt + "correctpassword");
    return new TenantAuthenticateInfo(
        "tenant-id-1", "testuser", password, salt, TenantStatus.ACTIVE, TenantRole.ADMIN);
  }

  @Test
  void register_returnsTokens() {
    final TenantAuthenticateInfo info = activeUser();
    when(tenantService.create(any())).thenReturn(info);
    when(jwtTokenService.createAccessToken(info)).thenReturn("access-token");
    when(jwtTokenService.createRefreshToken(info.id())).thenReturn("refresh-token");
    when(messagingApiKeyService.getMessagingApiKey()).thenReturn("global-api-key");

    final AuthResponse result = authService.register(new RegisterRequest("testuser", "pwd"));

    assertThat(result.token()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    assertThat(result.apiKey()).isEqualTo("global-api-key");
  }

  @Test
  void login_withCorrectPassword_returnsTokens() {
    final TenantAuthenticateInfo info = activeUser();
    when(tenantService.requireAuthenticateByUsername("testuser")).thenReturn(info);
    when(jwtTokenService.createAccessToken(info)).thenReturn("access-token");
    when(jwtTokenService.createRefreshToken(info.id())).thenReturn("refresh-token");
    when(messagingApiKeyService.getMessagingApiKey()).thenReturn("global-api-key");

    final AuthResponse result = authService.login(new AuthRequest("testuser", "correctpassword"));

    assertThat(result.token()).isEqualTo("access-token");
  }

  @Test
  void login_withWrongPassword_throwsUnauthorized() {
    final TenantAuthenticateInfo info = activeUser();
    when(tenantService.requireAuthenticateByUsername("testuser")).thenReturn(info);

    assertThatThrownBy(() -> authService.login(new AuthRequest("testuser", "wrongpassword")))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void login_withSuspendedAccount_throwsUnauthorized() {
    final String salt = "testsalt1234abcd";
    final String password = DigestUtils.sha256Hex(salt + "pwd");
    final TenantAuthenticateInfo suspended =
        new TenantAuthenticateInfo(
            "id", "user", password, salt, TenantStatus.SUSPENDED, TenantRole.USER);
    when(tenantService.requireAuthenticateByUsername("user")).thenReturn(suspended);

    assertThatThrownBy(() -> authService.login(new AuthRequest("user", "pwd")))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("not active");
  }

  @Test
  void refresh_withValidToken_returnsNewTokens() {
    final TenantAuthenticateInfo info = activeUser();
    when(jwtTokenService.parseRefreshToken("valid-refresh")).thenReturn("tenant-id-1");
    when(tenantService.getAuthenticateInfoById("tenant-id-1")).thenReturn(info);
    when(jwtTokenService.createAccessToken(info)).thenReturn("new-access");
    when(jwtTokenService.createRefreshToken(info.id())).thenReturn("new-refresh");
    when(messagingApiKeyService.getMessagingApiKey()).thenReturn("global-api-key");

    final AuthResponse result = authService.refresh("valid-refresh");

    assertThat(result.token()).isEqualTo("new-access");
  }
}
