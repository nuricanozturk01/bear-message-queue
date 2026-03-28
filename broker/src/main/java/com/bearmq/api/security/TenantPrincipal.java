package com.bearmq.api.security;

import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.dto.TenantInfo;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record TenantPrincipal(TenantInfo tenantInfo) implements Principal {

  @Override
  public String getName() {
    return this.tenantInfo.username();
  }

  public Collection<GrantedAuthority> authorities() {
    final String roleName =
        this.tenantInfo.role() != null ? this.tenantInfo.role().name() : TenantRole.USER.name();
    return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
  }
}
