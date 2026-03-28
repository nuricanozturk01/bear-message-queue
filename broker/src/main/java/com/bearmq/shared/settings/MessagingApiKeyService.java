package com.bearmq.shared.settings;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessagingApiKeyService {

  private static final int API_KEY_RANDOM_LEN = 44;

  private final ApplicationSettingsRepository applicationSettingsRepository;

  @Transactional(readOnly = true)
  public String getMessagingApiKey() {
    return this.applicationSettingsRepository
        .findById(ApplicationSettings.SINGLETON_ID)
        .map(ApplicationSettings::getMessagingApiKey)
        .orElseThrow(() -> new IllegalStateException("Application settings not initialized"));
  }

  @Transactional(readOnly = true)
  public boolean matchesMessagingApiKey(final String candidate) {
    if (candidate == null || candidate.isBlank()) {
      return false;
    }
    return this.applicationSettingsRepository
        .findById(ApplicationSettings.SINGLETON_ID)
        .map(ApplicationSettings::getMessagingApiKey)
        .filter(candidate::equals)
        .isPresent();
  }

  @Transactional
  public String rotateMessagingApiKey() {
    final ApplicationSettings row =
        this.applicationSettingsRepository
            .findById(ApplicationSettings.SINGLETON_ID)
            .orElseGet(
                () ->
                    ApplicationSettings.builder()
                        .id(ApplicationSettings.SINGLETON_ID)
                        .messagingApiKey(this.generateKey())
                        .build());
    final String next = this.generateKey();
    row.setMessagingApiKey(next);
    this.applicationSettingsRepository.save(row);
    return next;
  }

  private String generateKey() {
    return String.format(
        "bearmqt-%s", RandomStringUtils.secure().next(API_KEY_RANDOM_LEN, true, false));
  }
}
