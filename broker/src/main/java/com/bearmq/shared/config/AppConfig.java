package com.bearmq.shared.config;

import java.security.SecureRandom;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
  @Bean
  public Random getRandom() {

    return new SecureRandom();
  }
}
