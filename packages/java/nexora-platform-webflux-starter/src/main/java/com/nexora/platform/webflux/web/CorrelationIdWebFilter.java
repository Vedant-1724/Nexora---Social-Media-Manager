package com.nexora.platform.webflux.web;

import com.nexora.platform.core.web.NexoraHeaderNames;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class CorrelationIdWebFilter implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String correlationId = exchange.getRequest().getHeaders().getFirst(NexoraHeaderNames.CORRELATION_ID);
    if (!StringUtils.hasText(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    final String resolvedCorrelationId = correlationId;

    ServerHttpRequest request = exchange.getRequest().mutate()
        .header(NexoraHeaderNames.CORRELATION_ID, resolvedCorrelationId)
        .build();

    exchange.getAttributes().put(
        NexoraRequestAttributes.REQUEST_CONTEXT,
        NexoraRequestContext.fromHeaders(
            resolvedCorrelationId,
            exchange.getRequest().getHeaders().getFirst(NexoraHeaderNames.USER_ID),
            exchange.getRequest().getHeaders().getFirst(NexoraHeaderNames.WORKSPACE_ID),
            exchange.getRequest().getHeaders().getFirst(NexoraHeaderNames.SCOPES),
            MDC.get("traceId")));
    exchange.getResponse().getHeaders().set(NexoraHeaderNames.CORRELATION_ID, resolvedCorrelationId);

    return chain.filter(exchange.mutate().request(request).build())
        .doFirst(() -> MDC.put("correlationId", resolvedCorrelationId))
        .doFinally(signalType -> MDC.remove("correlationId"));
  }
}
