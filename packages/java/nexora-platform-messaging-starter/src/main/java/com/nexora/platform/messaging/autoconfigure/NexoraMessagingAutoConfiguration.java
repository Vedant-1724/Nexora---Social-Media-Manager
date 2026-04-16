package com.nexora.platform.messaging.autoconfigure;

import com.nexora.platform.core.config.NexoraMessagingProperties;
import com.nexora.platform.core.config.NexoraServiceProperties;
import com.nexora.platform.core.messaging.NexoraEventPublisher;
import com.nexora.platform.core.messaging.NexoraMessageSnapshotStore;
import com.nexora.platform.messaging.core.InMemoryMessageSnapshotStore;
import com.nexora.platform.messaging.core.RabbitBackedEventPublisher;
import com.nexora.platform.messaging.core.RabbitPlatformEventListener;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableRabbit
@EnableConfigurationProperties({NexoraServiceProperties.class, NexoraMessagingProperties.class})
public class NexoraMessagingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  MessageConverter rabbitMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  @ConditionalOnMissingBean
  NexoraMessageSnapshotStore nexoraMessageSnapshotStore() {
    return new InMemoryMessageSnapshotStore();
  }

  @Bean
  @ConditionalOnProperty(prefix = "nexora.messaging", name = "enabled", havingValue = "true")
  TopicExchange nexoraPlatformExchange(NexoraMessagingProperties properties) {
    return new TopicExchange(properties.getExchange(), true, false);
  }

  @Bean(name = "nexoraServiceQueue")
  @ConditionalOnProperty(prefix = "nexora.messaging", name = "enabled", havingValue = "true")
  Queue nexoraServiceQueue(NexoraMessagingProperties properties) {
    return QueueBuilder.durable(properties.getQueue()).build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "nexora.messaging", name = "enabled", havingValue = "true")
  Binding nexoraServiceBinding(
      Queue nexoraServiceQueue,
      TopicExchange nexoraPlatformExchange,
      NexoraMessagingProperties properties) {
    return BindingBuilder.bind(nexoraServiceQueue)
        .to(nexoraPlatformExchange)
        .with(properties.getRoutingKey());
  }

  @Bean
  @ConditionalOnProperty(prefix = "nexora.messaging", name = "enabled", havingValue = "true")
  NexoraEventPublisher nexoraEventPublisher(
      RabbitTemplate rabbitTemplate,
      NexoraServiceProperties serviceProperties,
      NexoraMessagingProperties messagingProperties) {
    return new RabbitBackedEventPublisher(rabbitTemplate, serviceProperties, messagingProperties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "nexora.messaging", name = "enabled", havingValue = "true")
  RabbitPlatformEventListener rabbitPlatformEventListener(
      ObjectProvider<NexoraMessageSnapshotStore> snapshotStore,
      NexoraMessagingProperties messagingProperties) {
    return new RabbitPlatformEventListener(snapshotStore.getIfAvailable(), messagingProperties);
  }
}
