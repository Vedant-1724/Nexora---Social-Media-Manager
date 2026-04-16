package com.nexora.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionRevocationStore {

  private final StringRedisTemplate redisTemplate;

  public AuthSessionRevocationStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void revokeRefreshSession(UUID sessionId, Instant expiresAt) {
    storeWithTtl(refreshSessionKey(sessionId), "revoked", expiresAt);
  }

  public boolean isRefreshSessionRevoked(UUID sessionId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(refreshSessionKey(sessionId)));
  }

  public void revokeAccessToken(String tokenId, Instant expiresAt) {
    storeWithTtl(accessTokenKey(tokenId), "revoked", expiresAt);
  }

  public boolean isAccessTokenRevoked(String tokenId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(accessTokenKey(tokenId)));
  }

  private void storeWithTtl(String key, String value, Instant expiresAt) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    if (ttl.isNegative() || ttl.isZero()) {
      ttl = Duration.ofMinutes(1);
    }
    redisTemplate.opsForValue().set(key, value, ttl);
  }

  private String refreshSessionKey(UUID sessionId) {
    return "auth:refresh:revoked:" + sessionId;
  }

  private String accessTokenKey(String tokenId) {
    return "auth:access:revoked:" + tokenId;
  }
}
