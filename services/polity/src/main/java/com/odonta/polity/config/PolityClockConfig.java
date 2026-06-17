package com.odonta.polity.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PolityClockConfig {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
