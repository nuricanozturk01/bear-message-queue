package com.bearmq.api.admin;

import com.bearmq.shared.tenant.Tenant;
import com.bearmq.shared.tenant.TenantRepository;
import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer {

  private static final int SALT_LENGTH = 16;
  private static final int RANDOM_PASSWORD_LENGTH = 12;

  private final TenantRepository tenantRepository;

  @Transactional
  @EventListener(ApplicationReadyEvent.class)
  public void ensureAdminExists() {
    if (this.tenantRepository.count() > 0) {
      return;
    }

    final String password = RandomStringUtils.secure().nextAlphanumeric(RANDOM_PASSWORD_LENGTH);
    final String salt = RandomStringUtils.secure().nextAlphanumeric(SALT_LENGTH);
    final String hashedPw = DigestUtils.sha256Hex(salt + password);

    final Tenant admin =
        Tenant.builder()
            .id(UlidCreator.getUlid().toString())
            .username("admin")
            .status(TenantStatus.ACTIVE)
            .role(TenantRole.ADMIN)
            .salt(salt)
            .password(hashedPw)
            .deleted(false)
            .build();

    this.tenantRepository.save(admin);

    log.warn(
        "======================================================\n"
            + " BearMQ first-run: admin user created.\n"
            + " Username : admin\n"
            + " Password : {}\n"
            + " Change this immediately in Settings.\n"
            + "======================================================",
        password);
  }
}
