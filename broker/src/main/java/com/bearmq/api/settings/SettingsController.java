package com.bearmq.api.settings;

import com.bearmq.api.auth.TenantContext;
import com.bearmq.shared.settings.MessagingApiKeyService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

  private final MessagingApiKeyService messagingApiKeyService;
  private final TenantContext tenantContext;

  @GetMapping("/messaging-api-key")
  public ResponseEntity<Map<String, String>> getMessagingApiKey() {
    this.tenantContext.requireTenant();
    return ResponseEntity.ok(Map.of("api_key", this.messagingApiKeyService.getMessagingApiKey()));
  }
}
