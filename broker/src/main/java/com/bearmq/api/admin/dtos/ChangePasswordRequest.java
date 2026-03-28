package com.bearmq.api.admin.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(@NotEmpty @Size(min = 6, max = 100) String newPassword) {}
