package com.bearmq.api.admin.services;

import com.bearmq.api.admin.dtos.ChangePasswordRequest;
import com.bearmq.api.admin.dtos.CreateUserRequest;
import com.bearmq.api.admin.dtos.UpdateRoleRequest;
import com.bearmq.api.admin.dtos.UserDto;
import com.bearmq.api.admin.mapper.AdminTenantMapper;
import com.bearmq.api.common.exceptions.ConflictException;
import com.bearmq.api.common.exceptions.ForbiddenException;
import com.bearmq.api.common.exceptions.UnauthorizedException;
import com.bearmq.api.security.MessagingApiPrincipalIds;
import com.bearmq.shared.tenant.Tenant;
import com.bearmq.shared.tenant.TenantRepository;
import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

  private static final int SALT_LENGTH = 16;

  private final TenantRepository tenantRepository;
  private final AdminTenantMapper adminTenantMapper;

  @Transactional(readOnly = true)
  public List<UserDto> listUsers() {

    return this.tenantRepository.findAll().stream()
        .filter(t -> !t.isDeleted())
        .map(this.adminTenantMapper::toUserDto)
        .toList();
  }

  @Transactional
  public UserDto createUser(final CreateUserRequest req) {

    final Optional<Tenant> existing = this.tenantRepository.findByUsername(req.username());

    if (existing.isEmpty()) {
      final String salt = RandomStringUtils.secure().nextAlphanumeric(SALT_LENGTH);
      final String hashedPw = DigestUtils.sha256Hex(salt + req.password());

      final Tenant tenant =
          Tenant.builder()
              .id(UlidCreator.getUlid().toString())
              .username(req.username())
              .status(TenantStatus.ACTIVE)
              .role(req.role() != null ? req.role() : TenantRole.USER)
              .salt(salt)
              .password(hashedPw)
              .deleted(false)
              .build();

      return this.adminTenantMapper.toUserDto(this.tenantRepository.save(tenant));
    }

    final Tenant t = existing.get();

    if (!t.isDeleted()) {
      throw new ConflictException("Username already registered");
    }

    this.applyNewPassword(t, req.password());
    t.setDeleted(false);
    t.setStatus(TenantStatus.ACTIVE);
    t.setRole(req.role() != null ? req.role() : TenantRole.USER);

    return this.adminTenantMapper.toUserDto(this.tenantRepository.save(t));
  }

  @Transactional
  public void changeUserPassword(final String userId, final ChangePasswordRequest req) {

    final Tenant tenant =
        this.tenantRepository
            .findById(userId)
            .filter(t -> !t.isDeleted())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    final String salt = RandomStringUtils.secure().nextAlphanumeric(SALT_LENGTH);
    final String hashedPw = DigestUtils.sha256Hex(salt + req.newPassword());

    tenant.setSalt(salt);
    tenant.setPassword(hashedPw);

    this.tenantRepository.save(tenant);
  }

  @Transactional
  public void changeOwnPassword(final String userId, final String currentRaw, final String newRaw) {

    if (MessagingApiPrincipalIds.TENANT_ID.equals(userId)) {
      throw new ForbiddenException("Password cannot be changed for this principal");
    }

    final Tenant tenant =
        this.tenantRepository
            .findById(userId)
            .filter(t -> !t.isDeleted())
            .orElseThrow(() -> new UnauthorizedException("User not found"));

    final String expectedHash = DigestUtils.sha256Hex(tenant.getSalt() + currentRaw);

    if (!expectedHash.equals(tenant.getPassword())) {
      throw new UnauthorizedException("Current password is incorrect");
    }

    final String newSalt = RandomStringUtils.secure().nextAlphanumeric(SALT_LENGTH);

    tenant.setSalt(newSalt);
    tenant.setPassword(DigestUtils.sha256Hex(newSalt + newRaw));

    this.tenantRepository.save(tenant);
  }

  @Transactional
  public UserDto updateRole(
      final String actorId, final String userId, final UpdateRoleRequest req) {

    if (actorId.equals(userId)) {
      throw new ForbiddenException("You cannot change your own role");
    }

    final Tenant tenant =
        this.tenantRepository
            .findById(userId)
            .filter(t -> !t.isDeleted())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (tenant.getRole() == TenantRole.ADMIN
        && req.role() != TenantRole.ADMIN
        && this.tenantRepository.countByRoleAndDeleted(TenantRole.ADMIN, false) <= 1) {

      throw new ForbiddenException("Cannot remove the last administrator");
    }

    tenant.setRole(req.role());
    return this.adminTenantMapper.toUserDto(this.tenantRepository.save(tenant));
  }

  @Transactional
  public void deleteUser(final String actorId, final String targetUserId) {

    if (actorId == null || targetUserId == null || actorId.isBlank() || targetUserId.isBlank()) {
      throw new ForbiddenException("Invalid delete request");
    }

    if (actorId.equals(targetUserId)) {
      throw new ForbiddenException("You cannot delete your own account");
    }

    final Tenant target =
        this.tenantRepository
            .findById(targetUserId)
            .filter(t -> !t.isDeleted())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (target.getRole() == TenantRole.ADMIN
        && this.tenantRepository.countByRoleAndDeleted(TenantRole.ADMIN, false) <= 1) {

      throw new ForbiddenException("Cannot delete the last administrator");
    }

    target.setDeleted(true);
    target.setStatus(TenantStatus.DELETED);
    this.tenantRepository.save(target);
  }

  private void applyNewPassword(final Tenant tenant, final String rawPassword) {

    final String salt = RandomStringUtils.secure().nextAlphanumeric(SALT_LENGTH);

    tenant.setSalt(salt);
    tenant.setPassword(DigestUtils.sha256Hex(salt + rawPassword));
  }
}
