package com.nexora.gateway.security;

import com.nexora.gateway.config.GatewaySecurityProperties;
import com.nexora.platform.core.web.NexoraHeaderNames;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class GatewayAuthenticationWebFilter implements WebFilter {

  private final GatewaySecurityProperties securityProperties;
  private final GatewayAccessTokenVerifier gatewayAccessTokenVerifier;
  private final GatewayAccessRevocationService gatewayAccessRevocationService;

  public GatewayAuthenticationWebFilter(
      GatewaySecurityProperties securityProperties,
      GatewayAccessTokenVerifier gatewayAccessTokenVerifier,
      GatewayAccessRevocationService gatewayAccessRevocationService) {
    this.securityProperties = securityProperties;
    this.gatewayAccessTokenVerifier = gatewayAccessTokenVerifier;
    this.gatewayAccessRevocationService = gatewayAccessRevocationService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    if (!path.startsWith("/api/v1")) {
      return chain.filter(exchange);
    }

    String authorizationHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
    boolean publicPath = isPublicPath(path);

    if (!StringUtils.hasText(authorizationHeader)) {
      if (publicPath) {
        return chain.filter(exchange);
      }
      return unauthorized(exchange);
    }

    String rawToken = extractBearerToken(authorizationHeader);
    GatewayAccessTokenVerifier.AuthenticatedGatewayRequest authenticatedRequest;
    try {
      authenticatedRequest = gatewayAccessTokenVerifier.verify(rawToken);
    } catch (RuntimeException exception) {
      return unauthorized(exchange);
    }

    if (authenticatedRequest.expiresAt().isBefore(Instant.now())) {
      return unauthorized(exchange);
    }

    return gatewayAccessRevocationService.isAccessTokenRevoked(authenticatedRequest.tokenId())
        .flatMap(revoked -> {
          if (Boolean.TRUE.equals(revoked)) {
            return unauthorized(exchange);
          }

          String scopesHeader =
              authenticatedRequest.scopes().stream().collect(Collectors.joining(","));
          ServerHttpRequest request =
              exchange.getRequest().mutate()
                  .header(NexoraHeaderNames.USER_ID, authenticatedRequest.userId().toString())
                  .header(NexoraHeaderNames.WORKSPACE_ID, authenticatedRequest.workspaceId().toString())
                  .header(NexoraHeaderNames.SCOPES, scopesHeader)
                  .build();

          exchange.getAttributes().put(
              NexoraRequestAttributes.REQUEST_CONTEXT,
              new NexoraRequestContext(
                  request.getHeaders().getFirst(NexoraHeaderNames.CORRELATION_ID),
                  authenticatedRequest.userId().toString(),
                  authenticatedRequest.workspaceId().toString(),
                  authenticatedRequest.scopes(),
                  MDC.get("traceId")));

          return chain.filter(exchange.mutate().request(request).build());
        });
  }

  private boolean isPublicPath(String path) {
    return securityProperties.getPublicPaths().stream().anyMatch(path::startsWith);
  }

  private String extractBearerToken(String authorizationHeader) {
    if (!authorizationHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Authorization header must use the Bearer scheme");
    }
    return authorizationHeader.substring("Bearer ".length()).trim();
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }
}
