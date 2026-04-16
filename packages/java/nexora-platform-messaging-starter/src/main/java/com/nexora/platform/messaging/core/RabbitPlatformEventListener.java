package com.nexora.platform.messaging.core;

import com.nexora.platform.core.api.MessageSnapshot;
import com.nexora.platform.core.api.PlatformEventMessage;
import com.nexora.platform.core.config.NexoraMessagingProperties;
import com.nexora.platform.core.messaging.NexoraMessageSnapshotStore;
import java.time.Instant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class RabbitPlatformEventListener {

  private final NexoraMessageSnapshotStore snapshotStore;
  private final NexoraMessagingProperties messagingProperties;

  public RabbitPlatformEventListener(
      NexoraMessageSnapshotStore snapshotStore,
      NexoraMessagingProperties messagingProperties) {
    this.snapshotStore = snapshotStore;
    this.messagingProperties = messagingProperties;
  }

  @RabbitListener(queues = "#{@nexoraServiceQueue.name}")
  public void onEvent(PlatformEventMessage eventMessage, Message message) {
    if (!messagingProperties.isConsumerEnabled()) {
      return;
    }

    snapshotStore.store(
        new MessageSnapshot(
            eventMessage.messageId(),
            eventMessage.type(),
            message.getMessageProperties().getReceivedRoutingKey(),
            eventMessage.source(),
            eventMessage.correlationId(),
            eventMessage.occurredAt(),
            Instant.now(),
            eventMessage.payload()));
  }
}
