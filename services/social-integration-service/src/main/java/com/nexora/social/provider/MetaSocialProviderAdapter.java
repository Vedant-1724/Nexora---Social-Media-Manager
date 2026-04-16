package com.nexora.social.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.social.config.SocialIntegrationProperties;
import com.nexora.social.config.SocialIntegrationProperties.ProviderSettings;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MetaSocialProviderAdapter implements SocialProviderAdapter {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private final SocialIntegrationProperties properties;
  private final ProviderSettings settings;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public MetaSocialProviderAdapter(
      SocialIntegrationProperties properties,
      RestClient socialRestClient,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.settings = properties.provider(SocialProvider.META);
    this.restClient = socialRestClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public SocialProvider provider() {
    return SocialProvider.META;
  }

  @Override
  public ProviderDescriptor descriptor() {
    return new ProviderDescriptor(
        SocialProvider.META,
        "Meta",
        settings.isEnabled(),
        false,
        properties.webhookPathFor(SocialProvider.META),
        List.copyOf(settings.getDefaultScopes()),
        List.copyOf(settings.getCapabilities()),
        List.of("youtube", "tiktok", "pinterest"));
  }

  @Override
  public String buildAuthorizationUrl(AuthorizationRequest request) {
    return settings.getAuthorizationUri()
        + "?client_id=" + encode(settings.getClientId())
        + "&redirect_uri=" + encode(request.callbackUri())
        + "&state=" + encode(request.state())
        + "&response_type=code"
        + "&scope=" + encode(String.join(",", request.scopes()));
  }

  @Override
  public OAuthCallbackResult exchangeAuthorizationCode(AuthorizationCodeExchange request) {
    JsonNode tokenResponse = restClient.get()
        .uri(UriComponentsBuilder.fromUriString(settings.getTokenUri())
            .queryParam("client_id", settings.getClientId())
            .queryParam("client_secret", settings.getClientSecret())
            .queryParam("redirect_uri", request.callbackUri())
            .queryParam("code", request.code())
            .build()
            .toUriString())
        .retrieve()
        .body(JsonNode.class);

    String accessToken = text(tokenResponse, "access_token");
    JsonNode profileResponse = restClient.get()
        .uri(UriComponentsBuilder.fromUriString(settings.getProfileUri())
            .queryParam("fields", "id,name")
            .queryParam("access_token", accessToken)
            .build()
            .toUriString())
        .retrieve()
        .body(JsonNode.class);

    String externalAccountId = text(profileResponse, "id");
    return new OAuthCallbackResult(
        new TokenBundle(
            accessToken,
            null,
            expiresAtFromSeconds(tokenResponse.path("expires_in").asLong(0)),
            List.copyOf(request.scopes())),
        new AccountProfile(
            externalAccountId,
            externalAccountId,
            "page",
            text(profileResponse, "name"),
            text(profileResponse, "username"),
            asMap(profileResponse)),
        List.copyOf(settings.getCapabilities()),
        new WebhookSubscriptionDescriptor(
            "meta-" + externalAccountId,
            settings.getClientSecret(),
            "active",
            Instant.now(),
            Map.of("provider", "meta")));
  }

  @Override
  public TokenRefreshResult refreshAccessToken(TokenRefreshCommand command) {
    String sourceToken = StringUtils.hasText(command.refreshToken()) ? command.refreshToken() : command.accessToken();
    JsonNode tokenResponse = restClient.get()
        .uri(UriComponentsBuilder.fromUriString(settings.getRefreshUri())
            .queryParam("grant_type", "fb_exchange_token")
            .queryParam("client_id", settings.getClientId())
            .queryParam("client_secret", settings.getClientSecret())
            .queryParam("fb_exchange_token", sourceToken)
            .build()
            .toUriString())
        .retrieve()
        .body(JsonNode.class);

    return new TokenRefreshResult(
        new TokenBundle(
            text(tokenResponse, "access_token"),
            command.refreshToken(),
            expiresAtFromSeconds(tokenResponse.path("expires_in").asLong(0)),
            command.scopes()),
        command.metadata());
  }

  @Override
  public PublicationResult publish(PublicationCommand command) {
    MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
    payload.add("message", command.message());
    payload.add("access_token", command.accessToken());
    if (StringUtils.hasText(command.linkUrl())) {
      payload.add("link", command.linkUrl());
    }

    JsonNode response = restClient.post()
        .uri(settings.getPublishUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(payload)
        .retrieve()
        .body(JsonNode.class);

    String externalPostId = text(response, "id");
    return new PublicationResult(
        externalPostId,
        "https://www.facebook.com/" + externalPostId,
        Instant.now(),
        asMap(response));
  }

  @Override
  public WebhookChallengeResponse handleWebhookChallenge(Map<String, String> queryParameters) {
    if (!"subscribe".equalsIgnoreCase(queryParameters.get("hub.mode"))) {
      throw new IllegalArgumentException("Unsupported Meta webhook mode");
    }
    if (!settings.getWebhookVerifyToken().equals(queryParameters.get("hub.verify_token"))) {
      throw new IllegalArgumentException("Meta webhook verify token does not match");
    }
    return new WebhookChallengeResponse(
        true,
        MediaType.TEXT_PLAIN_VALUE,
        queryParameters.get("hub.challenge"),
        Map.of());
  }

  @Override
  public WebhookEventResult parseWebhookEvent(Map<String, String> headers, String rawBody) {
    String signature = headers.getOrDefault("x-hub-signature-256", headers.get("X-Hub-Signature-256"));
    if (!verifyHexHmac(signature, rawBody, settings.getClientSecret())) {
      return new WebhookEventResult(false, "Meta webhook signature is invalid", List.of());
    }

    JsonNode payload = readTree(rawBody);
    List<NormalizedInboundEvent> events = new ArrayList<>();
    for (JsonNode entry : payload.path("entry")) {
      String accountId = text(entry, "id");
      for (JsonNode change : entry.path("changes")) {
        JsonNode value = change.path("value");
        String item = text(value, "item");
        String eventType = "comment";
        if ("mention".equalsIgnoreCase(item)) {
          eventType = "mention";
        }
        events.add(
            new NormalizedInboundEvent(
                accountId,
                accountId,
                stableEventId(accountId, value),
                text(value, "parent_id"),
                eventType,
                text(value, "from", "name"),
                asMap(value),
                Instant.now()));
      }
      for (JsonNode message : entry.path("messaging")) {
        events.add(
            new NormalizedInboundEvent(
                accountId,
                accountId,
                stableEventId(accountId, message),
                text(message, "message", "mid"),
                "dm",
                text(message, "sender", "id"),
                asMap(message),
                Instant.now()));
      }
    }

    return new WebhookEventResult(true, null, events);
  }

  private Instant expiresAtFromSeconds(long expiresIn) {
    return expiresIn <= 0 ? null : Instant.now().plusSeconds(expiresIn);
  }

  private String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private JsonNode readTree(String rawBody) {
    try {
      return objectMapper.readTree(rawBody == null ? "{}" : rawBody);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Unable to parse Meta webhook payload", exception);
    }
  }

  private Map<String, Object> asMap(JsonNode node) {
    return objectMapper.convertValue(node == null ? objectMapper.createObjectNode() : node, MAP_TYPE);
  }

  private String text(JsonNode node, String fieldName) {
    return text(node, fieldName, null);
  }

  private String text(JsonNode node, String fieldName, String nestedFieldName) {
    if (node == null || node.isMissingNode()) {
      return null;
    }
    JsonNode resolved = node.path(fieldName);
    if (nestedFieldName != null && resolved.isObject()) {
      resolved = resolved.path(nestedFieldName);
    }
    return resolved.isMissingNode() || resolved.isNull() ? null : resolved.asText();
  }

  private String stableEventId(String accountId, JsonNode payload) {
    return accountId + "-" + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
  }

  private boolean verifyHexHmac(String signatureHeader, String body, String secret) {
    if (!StringUtils.hasText(signatureHeader) || !signatureHeader.startsWith("sha256=")) {
      return false;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String expected = HexFormat.of().formatHex(mac.doFinal((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)));
      return expected.equals(signatureHeader.substring("sha256=".length()));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to verify Meta webhook signature", exception);
    }
  }
}
