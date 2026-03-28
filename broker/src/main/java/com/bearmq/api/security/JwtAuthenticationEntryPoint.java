package com.bearmq.api.security;

import com.bearmq.api.common.dtos.ApiErrorResponse;
import com.bearmq.api.common.mapper.ApiErrorResponseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;
  private final ApiErrorResponseMapper apiErrorResponseMapper;

  @Override
  public void commence(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AuthenticationException authException)
      throws IOException {

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    final ApiErrorResponse body =
        this.apiErrorResponseMapper.simple(
            401,
            "Unauthorized",
            authException.getMessage() != null
                ? authException.getMessage()
                : "Authentication required",
            request.getRequestURI());

    this.objectMapper.writeValue(response.getOutputStream(), body);
  }
}
