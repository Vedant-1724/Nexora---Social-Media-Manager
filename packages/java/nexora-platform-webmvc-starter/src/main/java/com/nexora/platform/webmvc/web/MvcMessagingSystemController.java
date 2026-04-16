package com.nexora.platform.webmvc.web;

import com.nexora.platform.core.api.MessagePublishRequest;
import com.nexora.platform.core.api.MessageSnapshot;
import com.nexora.platform.core.api.MessagingStatusResponse;
import com.nexora.platform.core.config.NexoraMessagingProperties;
import com.nexora.platform.core.messaging.NexoraEventPublisher;
import com.nexora.platform.core.messaging.NexoraMessageSnapshotStore;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/messaging")
public class MvcMessagingSystemController {

  private final NexoraMessagingProperties messagingProperties;
  private final ObjectProvider<NexoraEventPublisher> eventPublisher;
  private final ObjectProvider<NexoraMessageSnapshotStore> snapshotStore;

  public MvcMessagingSystemController(
      NexoraMessagingProperties messagingProperties,
      ObjectProvider<NexoraEventPublisher> eventPublisher,
      ObjectProvider<NexoraMessageSnapshotStore> snapshotStore) {
    this.messagingProperties = messagingProperties;
    this.eventPublisher = eventPublisher;
    this.snapshotStore = snapshotStore;
  }

  @GetMapping("/status")
  public MessagingStatusResponse status() {
    return new MessagingStatusResponse(
        messagingProperties.isEnabled(),
        messagingProperties.getExchange(),
        messagingProperties.getQueue(),
        messagingProperties.getRoutingKey(),
        currentSnapshot().orElse(null));
  }

  @GetMapping("/last-consumed")
  public ResponseEntity<MessageSnapshot> lastConsumed() {
    return currentSnapshot()
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PostMapping("/publish")
  public ResponseEntity<MessageSnapshot> publish(
      @Valid @RequestBody MessagePublishRequest request,
      HttpServletRequest httpServletRequest) {
    NexoraEventPublisher publisher = eventPublisher.getIfAvailable();
    if (publisher == null || !messagingProperties.isEnabled()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    NexoraRequestContext requestContext =
        (NexoraRequestContext) httpServletRequest.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);

    MessageSnapshot snapshot =
        publisher.publish(request, requestContext != null ? requestContext.correlationId() : null);
    return ResponseEntity.accepted().body(snapshot);
  }

  private Optional<MessageSnapshot> currentSnapshot() {
    NexoraMessageSnapshotStore store = snapshotStore.getIfAvailable();
    return store == null ? Optional.empty() : store.current();
  }
}
