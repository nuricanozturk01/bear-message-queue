package com.bearmq.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bearmq.api.admin.dto.CreateUserRequest;
import com.bearmq.api.common.exception.ConflictException;
import com.bearmq.api.common.exception.ForbiddenException;
import com.bearmq.shared.tenant.Tenant;
import com.bearmq.shared.tenant.TenantRepository;
import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

  @Mock private TenantRepository tenantRepository;

  @InjectMocks private AdminService adminService;

  @Test
  void deleteUser_whenActorDeletesSelf_throwsForbidden() {
    assertThatThrownBy(() -> this.adminService.deleteUser("same-id", "same-id"))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("own account");
    verify(this.tenantRepository, never()).findById(any());
    verify(this.tenantRepository, never()).save(any());
  }

  @Test
  void createUser_whenUsernameOnlySoftDeleted_reactivatesSameRow() {
    final Tenant deleted =
        Tenant.builder()
            .id("t1")
            .username("alice")
            .salt("oldsalt")
            .password("oldhash")
            .status(TenantStatus.DELETED)
            .role(TenantRole.USER)
            .deleted(true)
            .build();
    deleted.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));

    when(this.tenantRepository.findByUsername("alice")).thenReturn(Optional.of(deleted));
    when(this.tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

    final var dto =
        this.adminService.createUser(
            new CreateUserRequest("alice", "newsecret12", TenantRole.ADMIN));

    assertThat(dto.username()).isEqualTo("alice");
    assertThat(dto.role()).isEqualTo(TenantRole.ADMIN);
    final ArgumentCaptor<Tenant> cap = ArgumentCaptor.forClass(Tenant.class);
    verify(this.tenantRepository).save(cap.capture());
    assertThat(cap.getValue().isDeleted()).isFalse();
    assertThat(cap.getValue().getStatus()).isEqualTo(TenantStatus.ACTIVE);
  }

  @Test
  void createUser_whenUsernameActive_throwsConflict() {
    final Tenant active =
        Tenant.builder()
            .id("t2")
            .username("bob")
            .salt("s")
            .password("p")
            .status(TenantStatus.ACTIVE)
            .role(TenantRole.USER)
            .deleted(false)
            .build();
    when(this.tenantRepository.findByUsername("bob")).thenReturn(Optional.of(active));

    assertThatThrownBy(
            () ->
                this.adminService.createUser(
                    new CreateUserRequest("bob", "password1", TenantRole.USER)))
        .isInstanceOf(ConflictException.class);
  }
}
