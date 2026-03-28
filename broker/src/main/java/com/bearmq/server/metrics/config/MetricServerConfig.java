package com.bearmq.server.metrics.config;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricServerConfig {
  private static final int METRIC_THREAD_POOL_SIZE = 10;

  @Bean
  @ConditionalOnProperty(value = "bearmq.server.metrics.enabled", havingValue = "true")
  public ScheduledExecutorService provideMetricsScheduledThreadPool() {

    return Executors.newScheduledThreadPool(METRIC_THREAD_POOL_SIZE);
  }

  @Bean
  @ConditionalOnProperty(value = "bearmq.server.metrics.enabled", havingValue = "true")
  public DatagramSocket provideDatagramSocket(
      @Value("${bearmq.server.metrics.port}") final int port) throws IOException {
    return new DatagramSocket(port);
  }
}
