package com.bearmq.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bearmq.security.jwt")
public record JwtProperties(
    String secret, int accessTokenExpirationMinutes, int refreshTokenExpirationDays) {}
