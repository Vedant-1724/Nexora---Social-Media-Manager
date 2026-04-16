package com.nexora.platform.messaging.core;

import com.nexora.platform.core.api.MessagePublishRequest;
import com.nexora.platform.core.api.MessageSnapshot;
import com.nexora.platform.core.api.PlatformEventMessage;
import com.nexora.platform.core.config.NexoraMessagingProperties;
import com.nexora.platform.core.config.NexoraServiceProperties;
import com.nexora.platform.core.messaging.NexoraEventPublisher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.StringUtils;

public class RabbitBackedEventPublisher implements NexoraEventPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final NexoraServiceProperties serviceProperties;
  private final NexoraMessagingProperties messagingProperties;

  public RabbitBackedEventPublisher(
      RabbitTemplate rabbitTemplate,
      NexoraServiceProperties serviceProperties,
      NexoraMessagingProperties messagingProperties) {
    this.rabbitTemplate = rabbitTemplate;
    this.serviceProperties = serviceProperties;
    this.messagingProperties = messagingProperties;
  }

  @Override
  public MessageSnapshot publish(MessagePublishRequest request, String correlationId) {
    String routingKey =
        StringUtils.hasText(request.routingKey())
            ? request.routingKey()
            : messagingProperties.getRoutingKey();
    String source =
        StringUtils.hasText(request.source())
            ? request.source()
            : serviceProperties.getName();
    Instant occurredAt = Instant.now();
    PlatformEventMessage eventMessage =
        new PlatformEventMessage(
            UUID.randomUUID().toString(),
            request.type(),
            source,
            correlationId,
            occurredAt,
            request.payload() == null ? Map.of() : request.payload());

    rabbitTemplate.convertAndSend(messagingProperties.getExchange(), routingKey, eventMessage);
    return new MessageSnapshot(
        eventMessage.messageId(),
        eventMessage.type(),
        routingKey,
        source,
        eventMessage.correlationId(),
        occurredAt,
        null,
        eventMessage.payload());
  }
}
