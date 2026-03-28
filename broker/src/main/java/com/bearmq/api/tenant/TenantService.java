package com.bearmq.api.tenant;

import com.bearmq.api.auth.dto.RegisterRequest;
import com.bearmq.api.common.exceptions.ConflictException;
import com.bearmq.api.common.exceptions.UnauthorizedException;
import com.bearmq.api.tenant.converter.TenantConverter;
import com.bearmq.api.tenant.dto.TenantAuthenticateInfo;
import com.bearmq.shared.tenant.Tenant;
import com.bearmq.shared.tenant.TenantRepository;
import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantService {

  private static final int SALT_LENGTH = 16;

  private final TenantRepository tenantRepository;
  private final TenantConverter tenantConverter;

  public TenantAuthenticateInfo create(final RegisterRequest request) {

    final Optional<Tenant> existing = this.tenantRepository.findByUsername(request.username());
    if (existing.isPresent()) {
      final Tenant t = existing.get();
      if (!t.isDeleted()) {
        throw new ConflictException("Username already registered");
      }
      this.applyNewPassword(t, request.password());
      t.setDeleted(false);
      t.setStatus(TenantStatus.ACTIVE);
      t.setRole(TenantRole.USER);
      return this.tenantConverter.toTenantAuthenticateInfo(this.tenantRepository.save(t));
    }

    final String salt = RandomStringUtils.secure().nextAlphanumeric(SALT_LENGTH);
    final String password = DigestUtils.sha256Hex(salt + request.password());

    final Tenant tenantObj =
        Tenant.builder()
            .id(UlidCreator.getUlid().toString())
            .username(request.username())
            .status(TenantStatus.ACTIVE)
            .role(TenantRole.USER)
            .salt(salt)
            .password(password)
            .deleted(false)
            .build();

    final Tenant savedTenant = this.tenantRepository.save(tenantObj);
    return this.tenantConverter.toTenantAuthenticateInfo(savedTenant);
  }

  public TenantAuthenticateInfo requireAuthenticateByUsername(final String username) {

    return this.tenantRepository
        .findByUsername(username)
        .filter(t -> !t.isDeleted())
        .map(this.tenantConverter::toTenantAuthenticateInfo)
        .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
  }

  public TenantAuthenticateInfo getAuthenticateInfoById(final String id) {

    return this.tenantRepository
        .findById(id)
        .filter(t -> !t.isDeleted())
        .map(this.tenantConverter::toTenantAuthenticateInfo)
        .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
  }

  private void applyNewPassword(final Tenant tenant, final String rawPassword) {

    final String salt = RandomStringUtils.secure().nextAlphanumeric(SALT_LENGTH);
    tenant.setSalt(salt);
    tenant.setPassword(DigestUtils.sha256Hex(salt + rawPassword));
  }
}
