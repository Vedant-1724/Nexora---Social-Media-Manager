package com.nexora.platform.webflux.autoconfigure;

import com.nexora.platform.core.config.NexoraMessagingProperties;
import com.nexora.platform.core.config.NexoraServiceProperties;
import com.nexora.platform.webflux.web.CorrelationIdWebFilter;
import com.nexora.platform.webflux.web.WebFluxApiExceptionHandler;
import com.nexora.platform.webflux.web.WebFluxMessagingSystemController;
import com.nexora.platform.webflux.web.WebFluxRequestContextController;
import com.nexora.platform.webflux.web.WebFluxServiceInfoController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties({NexoraServiceProperties.class, NexoraMessagingProperties.class})
@Import({
  WebFluxApiExceptionHandler.class,
  WebFluxServiceInfoController.class,
  WebFluxRequestContextController.class,
  WebFluxMessagingSystemController.class
})
public class NexoraWebFluxAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  CorrelationIdWebFilter correlationIdWebFilter() {
    return new CorrelationIdWebFilter();
  }

  @Bean
  @ConditionalOnMissingBean
  OpenAPI nexoraOpenApi(NexoraServiceProperties properties) {
    return new OpenAPI()
        .info(
            new Info()
                .title(properties.getDisplayName())
                .description(properties.getDescription())
                .version(properties.getVersion()));
  }
}
