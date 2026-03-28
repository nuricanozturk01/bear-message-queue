package com.bearmq.client.config;

import static com.bearmq.client.Names.CONFIG_BASE;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(CONFIG_BASE)
public class BearConfig {
  private static final int REST_PORT = 3333;
  private static final int DEFAULT_INITIAL_DELAY = 500;
  private static final int DEFAULT_POLLING_DELAY = 250;
  /** Prevents scheduleWithFixedDelay(…, 0, 0) busy-loop when the queue is hot or broker returns fast. */
  private static final int MIN_POLL_PERIOD_MS = 50;

  private String username;
  private String password;
  private String host;
  private int port;
  private String virtualHost;
  private String apiKey;
  private int initialDelayMs = DEFAULT_INITIAL_DELAY;
  private int periodMs = DEFAULT_POLLING_DELAY;

  public int getInitialDelayMs() {
    return initialDelayMs;
  }

  public void setInitialDelayMs(final int initialDelayMs) {
    this.initialDelayMs = Math.max(0, initialDelayMs);
  }

  public int getPeriodMs() {
    return periodMs;
  }

  public void setPeriodMs(final int periodMs) {
    if (periodMs <= 0) {
      this.periodMs = DEFAULT_POLLING_DELAY;
    } else {
      this.periodMs = Math.max(periodMs, MIN_POLL_PERIOD_MS);
    }
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(final String apiKey) {
    this.apiKey = apiKey;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  public String getVirtualHost() {
    return virtualHost;
  }

  public void setVirtualHost(final String virtualHost) {
    this.virtualHost = virtualHost;
  }

  public String getUrl() {
    if (host == null) {
      throw new IllegalStateException("host is null");
    } else if (host.contains("localhost") || host.contains("127.0.0.1") || host.contains("http")) {
      return String.format("http://%s:%d", host, REST_PORT);
    } else {
      return String.format("https://%s:%d", host, REST_PORT);
    }
  }
}
