package com.nexora.scheduler.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties(PostSchedulerProperties.class)
public class PostSchedulerConfiguration {

  @Bean
  Clock schedulerClock() {
    return Clock.systemUTC();
  }

  @Bean
  RestClient schedulerRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
