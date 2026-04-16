package com.nexora.gateway.security;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GatewayAccessRevocationService {

  private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

  public GatewayAccessRevocationService(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
    this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
  }

  public Mono<Boolean> isAccessTokenRevoked(String tokenId) {
    return reactiveStringRedisTemplate.hasKey(accessTokenKey(tokenId))
        .defaultIfEmpty(false);
  }

  private String accessTokenKey(String tokenId) {
    return "auth:access:revoked:" + tokenId;
  }
}
