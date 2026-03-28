package com.bearmq.shared.tenant.dto;

import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;

public record TenantInfo(String id, String username, TenantStatus status, TenantRole role) {}
