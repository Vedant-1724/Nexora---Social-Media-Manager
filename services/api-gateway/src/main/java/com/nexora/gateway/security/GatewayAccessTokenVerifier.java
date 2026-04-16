package com.nexora.gateway.security;

import com.nexora.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class GatewayAccessTokenVerifier {

  private final GatewaySecurityProperties securityProperties;
  private final SecretKey secretKey;

  public GatewayAccessTokenVerifier(GatewaySecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
    this.secretKey = buildSecretKey(securityProperties.getJwt().getSecret());
  }

  public AuthenticatedGatewayRequest verify(String rawToken) {
    Claims claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(securityProperties.getJwt().getIssuer())
            .build()
            .parseSignedClaims(rawToken)
            .getPayload();

    @SuppressWarnings("unchecked")
    List<String> scopes = claims.get("scopes", List.class);

    return new AuthenticatedGatewayRequest(
        UUID.fromString(claims.getSubject()),
        claims.get("email", String.class),
        claims.get("displayName", String.class),
        UUID.fromString(claims.get("workspaceId", String.class)),
        claims.get("workspaceName", String.class),
        UUID.fromString(claims.get("sessionId", String.class)),
        claims.getId(),
        scopes == null ? List.of() : List.copyOf(scopes),
        claims.getExpiration().toInstant());
  }

  public String issueTestToken(
      UUID userId,
      UUID workspaceId,
      UUID sessionId,
      String tokenId,
      List<String> scopes,
      Instant expiresAt) {
    return Jwts.builder()
        .issuer(securityProperties.getJwt().getIssuer())
        .subject(userId.toString())
        .id(tokenId)
        .issuedAt(new Date())
        .expiration(Date.from(expiresAt))
        .claim("email", "test@nexora.dev")
        .claim("displayName", "Test User")
        .claim("workspaceId", workspaceId.toString())
        .claim("workspaceName", "Test Workspace")
        .claim("sessionId", sessionId.toString())
        .claim("scopes", scopes)
        .signWith(secretKey)
        .compact();
  }

  private SecretKey buildSecretKey(String rawSecret) {
    try {
      return Keys.hmacShaKeyFor(Decoders.BASE64.decode(rawSecret));
    } catch (RuntimeException ignored) {
      return Keys.hmacShaKeyFor(rawSecret.getBytes(StandardCharsets.UTF_8));
    }
  }

  public record AuthenticatedGatewayRequest(
      UUID userId,
      String email,
      String displayName,
      UUID workspaceId,
      String workspaceName,
      UUID sessionId,
      String tokenId,
      List<String> scopes,
      Instant expiresAt) {}
}
