package com.bearmq.api.admin.mapper;

import com.bearmq.api.admin.dtos.UserDto;
import com.bearmq.shared.tenant.Tenant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdminTenantMapper {

  UserDto toUserDto(Tenant tenant);
}
