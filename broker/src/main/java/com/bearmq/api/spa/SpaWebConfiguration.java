package com.bearmq.api.spa;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SpaWebConfiguration {

  @Bean
  public FilterRegistrationBean<SpaFallbackFilter> spaFallbackFilterRegistration() {

    final FilterRegistrationBean<SpaFallbackFilter> registration =
        new FilterRegistrationBean<SpaFallbackFilter>();
    registration.setFilter(new SpaFallbackFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.LOWEST_PRECEDENCE);
    return registration;
  }
}
