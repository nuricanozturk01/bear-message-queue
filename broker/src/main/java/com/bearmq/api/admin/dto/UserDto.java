package com.bearmq.api.admin.dto;

import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import java.time.Instant;

public record UserDto(
    String id, String username, TenantRole role, TenantStatus status, Instant createdAt) {}
