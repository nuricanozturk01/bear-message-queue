package com.bearmq.api.auth;

import com.bearmq.api.common.exception.UnauthorizedException;
import com.bearmq.api.security.TenantPrincipal;
import com.bearmq.shared.tenant.dto.TenantInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantContext {

  public TenantInfo requireTenant() {
    final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null
        || !auth.isAuthenticated()
        || !(auth.getPrincipal() instanceof TenantPrincipal principal)) {
      throw new UnauthorizedException("Authentication required");
    }
    return principal.tenantInfo();
  }
}
