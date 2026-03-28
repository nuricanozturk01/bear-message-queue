package com.bearmq.api.admin.controllers;

import com.bearmq.api.admin.dtos.ChangePasswordRequest;
import com.bearmq.api.admin.dtos.CreateUserRequest;
import com.bearmq.api.admin.dtos.UpdateRoleRequest;
import com.bearmq.api.admin.dtos.UserDto;
import com.bearmq.api.admin.services.AdminService;
import com.bearmq.api.auth.services.TenantContext;
import com.bearmq.shared.settings.MessagingApiKeyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;
  private final TenantContext tenantContext;
  private final MessagingApiKeyService messagingApiKeyService;

  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserDto>> listUsers() {

    return ResponseEntity.ok(this.adminService.listUsers());
  }

  @PostMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserDto> createUser(@Valid @RequestBody final CreateUserRequest req) {

    return ResponseEntity.ok(this.adminService.createUser(req));
  }

  @DeleteMapping("/users/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteUser(@PathVariable final String userId) {

    final String actorId = this.tenantContext.requireTenant().id();

    this.adminService.deleteUser(actorId, userId);

    return ResponseEntity.noContent().build();
  }

  @PutMapping("/users/{userId}/password")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> changeUserPassword(
      @PathVariable final String userId, @Valid @RequestBody final ChangePasswordRequest req) {

    this.adminService.changeUserPassword(userId, req);

    return ResponseEntity.noContent().build();
  }

  @PutMapping("/users/{userId}/role")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserDto> updateRole(
      @PathVariable final String userId, @Valid @RequestBody final UpdateRoleRequest req) {

    final String actorId = this.tenantContext.requireTenant().id();

    return ResponseEntity.ok(this.adminService.updateRole(actorId, userId, req));
  }

  @PostMapping("/messaging-api-key/rotate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> rotateMessagingApiKey() {

    final String next = this.messagingApiKeyService.rotateMessagingApiKey();

    return ResponseEntity.ok(Map.of("api_key", next));
  }
}
