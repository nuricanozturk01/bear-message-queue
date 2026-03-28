package com.bearmq.api.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotEmpty @Size(min = 3, max = 150) String username,
    @NotEmpty @Size(min = 5, max = 50) String password) {}
