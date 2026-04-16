package com.nexora.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  GatewayDownstreamServicesProperties.class,
  GatewaySecurityProperties.class
})
public class GatewayPlatformConfiguration {}
