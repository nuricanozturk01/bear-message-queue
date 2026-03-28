package com.bearmq.api.admin.dtos;

import com.bearmq.shared.tenant.TenantRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotEmpty @Size(min = 3, max = 150) String username,
    @NotEmpty @Size(min = 6, max = 100) String password,
    @NotNull TenantRole role) {}
