package com.bearmq.api.tenant.converter;

import com.bearmq.api.tenant.dto.TenantAuthenticateInfo;
import com.bearmq.shared.tenant.Tenant;
import com.bearmq.shared.tenant.dto.TenantInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantConverter {
  TenantInfo toTenantInfo(Tenant tenant);

  TenantAuthenticateInfo toTenantAuthenticateInfo(Tenant tenant);
}
