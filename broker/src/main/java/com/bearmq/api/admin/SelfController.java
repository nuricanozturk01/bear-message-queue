package com.bearmq.api.admin;

import com.bearmq.api.auth.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SelfController {

  private final AdminService adminService;
  private final TenantContext tenantContext;

  public record SelfPasswordRequest(
      @NotEmpty @Size(min = 1) String currentPassword,
      @NotEmpty @Size(min = 6, max = 100) String newPassword) {}

  @PutMapping("/me/password")
  public ResponseEntity<Void> changeOwnPassword(@Valid @RequestBody final SelfPasswordRequest req) {
    final String myId = this.tenantContext.requireTenant().id();
    this.adminService.changeOwnPassword(myId, req.currentPassword(), req.newPassword());
    return ResponseEntity.noContent().build();
  }
}
