package com.nexora.platform.webflux.web;

import com.nexora.platform.core.web.NexoraHeaderNames;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

@RestController
@RequestMapping("/api/v1/system")
public class WebFluxRequestContextController {

  @GetMapping("/request-context")
  public NexoraRequestContext requestContext(ServerWebExchange exchange) {
    NexoraRequestContext requestContext =
        exchange.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);
    if (requestContext != null) {
      return new NexoraRequestContext(
          requestContext.correlationId(),
          requestContext.userId(),
          requestContext.workspaceId(),
          requestContext.scopes(),
          MDC.get("traceId"));
    }

    return NexoraRequestContext.fromHeaders(
        exchange.getRequest().getHeaders().getFirst(NexoraHeaderNames.CORRELATION_ID),
        exchange.getRequest().getHeaders().getFirst(NexoraHeaderNames.USER_ID),
        exchange.getRequest().getHeaders().getFirst(NexoraHeaderNames.WORKSPACE_ID),
        exchange.getRequest().getHeaders().getFirst(NexoraHeaderNames.SCOPES),
        MDC.get("traceId"));
  }
}
