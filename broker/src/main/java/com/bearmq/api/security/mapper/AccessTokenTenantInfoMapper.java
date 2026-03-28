package com.bearmq.api.security.mapper;

import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import com.bearmq.shared.tenant.dto.TenantInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccessTokenTenantInfoMapper {

  TenantInfo toTenantInfo(String id, String username, TenantStatus status, TenantRole role);
}
