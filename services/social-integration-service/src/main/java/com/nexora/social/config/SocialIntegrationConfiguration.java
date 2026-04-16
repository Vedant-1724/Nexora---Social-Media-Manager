package com.nexora.social.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestClient;

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(SocialIntegrationProperties.class)
public class SocialIntegrationConfiguration {

  @Bean
  Clock socialClock() {
    return Clock.systemUTC();
  }

  @Bean
  RestClient socialRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
