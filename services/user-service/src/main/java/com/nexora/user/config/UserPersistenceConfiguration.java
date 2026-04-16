package com.nexora.user.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class UserPersistenceConfiguration {
  @Bean
  Clock userClock() {
    return Clock.systemUTC();
  }
}
