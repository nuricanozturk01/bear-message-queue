package com.bearmq.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AuthResponse(
    @JsonProperty("access_token") String token,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("api_key") String apiKey) {}
