package com.bearmq.shared.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "application_settings")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ApplicationSettings {

  public static final String SINGLETON_ID = "default";

  @Id
  @Column(nullable = false, length = 32)
  private String id;

  @Column(name = "messaging_api_key", nullable = false, length = 64)
  private String messagingApiKey;
}
