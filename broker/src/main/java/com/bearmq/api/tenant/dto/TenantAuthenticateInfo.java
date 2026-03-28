package com.bearmq.api.tenant.dto;

import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;

public record TenantAuthenticateInfo(
    String id,
    String username,
    String password,
    String salt,
    TenantStatus status,
    TenantRole role) {}
