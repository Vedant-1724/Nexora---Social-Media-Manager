package com.nexora.platform.webmvc.autoconfigure;

import com.nexora.platform.core.config.NexoraMessagingProperties;
import com.nexora.platform.core.config.NexoraServiceProperties;
import com.nexora.platform.webmvc.web.MvcAuthorizationInterceptor;
import com.nexora.platform.webmvc.web.CorrelationIdFilter;
import com.nexora.platform.webmvc.web.MvcApiExceptionHandler;
import com.nexora.platform.webmvc.web.MvcMessagingSystemController;
import com.nexora.platform.webmvc.web.MvcRequestContextController;
import com.nexora.platform.webmvc.web.MvcServiceInfoController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@EnableConfigurationProperties({NexoraServiceProperties.class, NexoraMessagingProperties.class})
@Import({
  MvcApiExceptionHandler.class,
  MvcServiceInfoController.class,
  MvcRequestContextController.class,
  MvcMessagingSystemController.class
})
public class NexoraWebMvcAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
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

  @Bean
  @ConditionalOnMissingBean
  MvcAuthorizationInterceptor mvcAuthorizationInterceptor() {
    return new MvcAuthorizationInterceptor();
  }

  @Bean
  @ConditionalOnMissingBean
  WebMvcConfigurer nexoraAuthorizationConfigurer(
      MvcAuthorizationInterceptor authorizationInterceptor) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor);
      }
    };
  }
}
