package com.bearmq.api.security;

import com.bearmq.api.common.dto.ApiErrorResponse;
import com.bearmq.shared.settings.MessagingApiKeyService;
import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import com.bearmq.shared.tenant.dto.TenantInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class TenantAuthenticationFilter extends OncePerRequestFilter {

  private static final String API_KEY_HEADER = "X-API-KEY";

  private static final TenantInfo MESSAGING_API_TENANT =
      new TenantInfo(
          MessagingApiPrincipalIds.TENANT_ID, "api-client", TenantStatus.ACTIVE, TenantRole.USER);

  private final JwtTokenService jwtTokenService;
  private final MessagingApiKeyService messagingApiKeyService;
  private final ObjectMapper objectMapper;

  @Override
  protected boolean shouldNotFilter(final HttpServletRequest request) {
    final String path = request.getServletPath();
    return path.startsWith("/api/auth/login") || path.startsWith("/api/auth/refresh");
  }

  @Override
  protected void doFilterInternal(
      final @NonNull HttpServletRequest request,
      final @NonNull HttpServletResponse response,
      final @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    final String apiKeyHeader = request.getHeader(API_KEY_HEADER);

    if (hasBearer(authHeader)) {
      if (!this.processBearer(request, response, authHeader, apiKeyHeader)) {
        return;
      }
    } else if (StringUtils.hasText(apiKeyHeader)) {
      if (this.messagingApiKeyService.matchesMessagingApiKey(apiKeyHeader)) {
        this.authenticate(request, MESSAGING_API_TENANT);
      } else {
        this.writeError(response, request, HttpStatus.UNAUTHORIZED, "Invalid API key");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private static boolean hasBearer(final String authHeader) {
    return StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ");
  }

  private boolean processBearer(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final String authHeader,
      final String apiKeyHeader)
      throws IOException {
    final String token = authHeader.substring("Bearer ".length());
    if (!this.jwtTokenService.isLikelyJwt(token)) {
      this.writeError(response, request, HttpStatus.UNAUTHORIZED, "Invalid bearer token");
      return false;
    }
    try {
      final TenantInfo info = this.jwtTokenService.parseAccessToken(token);
      if (!this.verifyApiKeyIfPresent(apiKeyHeader)) {
        this.writeError(
            response, request, HttpStatus.FORBIDDEN, "API key does not match configured key");
        return false;
      }
      this.authenticate(request, info);
    } catch (final JwtException ex) {
      this.writeError(response, request, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
      return false;
    }
    return true;
  }

  private boolean verifyApiKeyIfPresent(final String apiKeyHeader) {
    if (!StringUtils.hasText(apiKeyHeader)) {
      return true;
    }
    return this.messagingApiKeyService.matchesMessagingApiKey(apiKeyHeader);
  }

  private void authenticate(final HttpServletRequest request, final TenantInfo tenant) {
    final TenantPrincipal principal = new TenantPrincipal(tenant);
    final UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void writeError(
      final HttpServletResponse response,
      final HttpServletRequest request,
      final HttpStatus status,
      final String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    final ApiErrorResponse body =
        ApiErrorResponse.of(
            status.value(), status.getReasonPhrase(), message, request.getRequestURI());
    this.objectMapper.writeValue(response.getOutputStream(), body);
  }
}
