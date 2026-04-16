package com.nexora.auth.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestClient;

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(AuthSecurityProperties.class)
public class AuthInfrastructureConfiguration {

  @Bean
  Clock authClock() {
    return Clock.systemUTC();
  }

  @Bean
  RestClient authRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
