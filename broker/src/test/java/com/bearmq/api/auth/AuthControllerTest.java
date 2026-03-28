package com.bearmq.api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bearmq.api.auth.dto.AuthResponse;
import com.bearmq.api.common.exception.UnauthorizedException;
import com.bearmq.api.security.JwtAuthenticationEntryPoint;
import com.bearmq.api.security.TenantAuthenticationFilter;
import com.bearmq.server.broker.runner.BrokerServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JwtAuthenticationEntryPoint.class})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthService authService;
  @MockitoBean private TenantAuthenticationFilter tenantAuthenticationFilter;
  @MockitoBean private BrokerServer brokerServer;

  @Test
  void login_withValidCredentials_returns200WithTokens() throws Exception {
    final AuthResponse response =
        AuthResponse.builder().token("acc-token").refreshToken("ref-token").apiKey("key").build();
    when(authService.login(any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user\",\"password\":\"password\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").value("acc-token"))
        .andExpect(jsonPath("$.refresh_token").value("ref-token"));
  }

  @Test
  void login_withInvalidCredentials_returns401() throws Exception {
    when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid credentials"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }
}
