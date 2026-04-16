package com.nexora.platform.webflux.web;

import com.nexora.platform.core.api.ServiceInfoResponse;
import com.nexora.platform.core.config.NexoraMessagingProperties;
import com.nexora.platform.core.config.NexoraServiceProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class WebFluxServiceInfoController {

  private final NexoraServiceProperties serviceProperties;
  private final NexoraMessagingProperties messagingProperties;

  public WebFluxServiceInfoController(
      NexoraServiceProperties serviceProperties,
      NexoraMessagingProperties messagingProperties) {
    this.serviceProperties = serviceProperties;
    this.messagingProperties = messagingProperties;
  }

  @GetMapping("/info")
  public ServiceInfoResponse info() {
    return new ServiceInfoResponse(
        serviceProperties.getName(),
        serviceProperties.getDisplayName(),
        serviceProperties.getDescription(),
        serviceProperties.getPhase(),
        serviceProperties.getVersion(),
        serviceProperties.getEnvironment(),
        serviceProperties.getDocsPath(),
        Map.of(
            "messagingEnabled", messagingProperties.isEnabled(),
            "queue", messagingProperties.getQueue(),
            "routingKey", messagingProperties.getRoutingKey()));
  }
}
