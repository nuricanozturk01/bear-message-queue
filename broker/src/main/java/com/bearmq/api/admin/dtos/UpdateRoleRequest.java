package com.bearmq.api.admin.dtos;

import com.bearmq.shared.tenant.TenantRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull TenantRole role) {}
