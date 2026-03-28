package com.bearmq.server.broker.config;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@Configuration
@Lazy
public class BrokerServerConfig {
  @Bean
  public ServerSocket provideServerSocket(
      @Value("${bearmq.server.broker.port}") final int port,
      @Value("${bearmq.server.broker.backlog}") final int backlog)
      throws IOException {
    return new ServerSocket(port, backlog);
  }

  @Bean
  @Scope("prototype")
  @Primary
  public ExecutorService provideCachedThreadPool() {

    return Executors.newCachedThreadPool();
  }

  @Bean("thread.virtual")
  public ExecutorService provideVirtualThreadExecutor() {

    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
