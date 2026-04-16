package com.nexora.platform.core.messaging;

import com.nexora.platform.core.api.MessagePublishRequest;
import com.nexora.platform.core.api.MessageSnapshot;

public interface NexoraEventPublisher {

  MessageSnapshot publish(MessagePublishRequest request, String correlationId);
}
