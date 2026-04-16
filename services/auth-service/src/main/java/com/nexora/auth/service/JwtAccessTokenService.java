package com.nexora.auth.service;

import com.nexora.auth.config.AuthSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtAccessTokenService {

  private final AuthSecurityProperties authSecurityProperties;
  private final Clock clock;
  private final SecretKey secretKey;

  public JwtAccessTokenService(AuthSecurityProperties authSecurityProperties, Clock authClock) {
    this.authSecurityProperties = authSecurityProperties;
    this.clock = authClock;
    this.secretKey = buildSecretKey(authSecurityProperties.getJwt().getSecret());
  }

  public IssuedToken issueToken(AccessTokenIdentity identity) {
    Instant issuedAt = Instant.now(clock);
    Instant expiresAt = issuedAt.plus(authSecurityProperties.getJwt().getAccessTokenTtl());
    String tokenId = UUID.randomUUID().toString();

    String token =
        Jwts.builder()
            .issuer(authSecurityProperties.getJwt().getIssuer())
            .subject(identity.userId().toString())
            .id(tokenId)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .claim("email", identity.email())
            .claim("displayName", identity.displayName())
            .claim("emailVerified", identity.emailVerified())
            .claim("workspaceId", identity.workspaceId().toString())
            .claim("workspaceName", identity.workspaceName())
            .claim("sessionId", identity.sessionId().toString())
            .claim("scopes", identity.scopes())
            .signWith(secretKey)
            .compact();

    return new IssuedToken(token, tokenId, expiresAt);
  }

  public ParsedAccessToken parse(String rawToken) {
    Claims claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(authSecurityProperties.getJwt().getIssuer())
            .build()
            .parseSignedClaims(rawToken)
            .getPayload();

    @SuppressWarnings("unchecked")
    List<String> scopes = claims.get("scopes", List.class);

    return new ParsedAccessToken(
        UUID.fromString(claims.getSubject()),
        claims.get("email", String.class),
        claims.get("displayName", String.class),
        claims.get("emailVerified", Boolean.class),
        UUID.fromString(claims.get("workspaceId", String.class)),
        claims.get("workspaceName", String.class),
        UUID.fromString(claims.get("sessionId", String.class)),
        scopes == null ? List.of() : List.copyOf(scopes),
        claims.getId(),
        claims.getExpiration().toInstant());
  }

  private SecretKey buildSecretKey(String rawSecret) {
    try {
      return Keys.hmacShaKeyFor(Decoders.BASE64.decode(rawSecret));
    } catch (RuntimeException ignored) {
      return Keys.hmacShaKeyFor(rawSecret.getBytes(StandardCharsets.UTF_8));
    }
  }

  public record AccessTokenIdentity(
      UUID userId,
      String email,
      String displayName,
      boolean emailVerified,
      UUID workspaceId,
      String workspaceName,
      UUID sessionId,
      List<String> scopes) {}

  public record IssuedToken(String token, String tokenId, Instant expiresAt) {}

  public record ParsedAccessToken(
      UUID userId,
      String email,
      String displayName,
      boolean emailVerified,
      UUID workspaceId,
      String workspaceName,
      UUID sessionId,
      List<String> scopes,
      String tokenId,
      Instant expiresAt) {}
}
