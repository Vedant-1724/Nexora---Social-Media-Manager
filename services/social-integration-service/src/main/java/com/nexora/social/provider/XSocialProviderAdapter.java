package com.nexora.social.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.social.config.SocialIntegrationProperties;
import com.nexora.social.config.SocialIntegrationProperties.ProviderSettings;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class XSocialProviderAdapter implements SocialProviderAdapter {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private final SocialIntegrationProperties properties;
  private final ProviderSettings settings;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public XSocialProviderAdapter(
      SocialIntegrationProperties properties,
      RestClient socialRestClient,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.settings = properties.provider(SocialProvider.X);
    this.restClient = socialRestClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public SocialProvider provider() {
    return SocialProvider.X;
  }

  @Override
  public ProviderDescriptor descriptor() {
    return new ProviderDescriptor(
        SocialProvider.X,
        "X",
        settings.isEnabled(),
        true,
        properties.webhookPathFor(SocialProvider.X),
        List.copyOf(settings.getDefaultScopes()),
        List.copyOf(settings.getCapabilities()),
        List.of("youtube", "tiktok", "pinterest"));
  }

  @Override
  public String buildAuthorizationUrl(AuthorizationRequest request) {
    return settings.getAuthorizationUri()
        + "?response_type=code"
        + "&client_id=" + encode(settings.getClientId())
        + "&redirect_uri=" + encode(request.callbackUri())
        + "&state=" + encode(request.state())
        + "&scope=" + encode(String.join(" ", request.scopes()))
        + "&code_challenge=" + encode(codeChallenge(request.codeVerifier()))
        + "&code_challenge_method=S256";
  }

  @Override
  public OAuthCallbackResult exchangeAuthorizationCode(AuthorizationCodeExchange request) {
    MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
    payload.add("grant_type", "authorization_code");
    payload.add("code", request.code());
    payload.add("redirect_uri", request.callbackUri());
    payload.add("client_id", settings.getClientId());
    payload.add("code_verifier", request.codeVerifier());

    JsonNode tokenResponse = restClient.post()
        .uri(settings.getTokenUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .header(HttpHeaders.AUTHORIZATION, basicAuth())
        .body(payload)
        .retrieve()
        .body(JsonNode.class);

    String accessToken = text(tokenResponse, "access_token");
    JsonNode profileResponse = restClient.get()
        .uri(settings.getProfileUri())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .body(JsonNode.class);

    JsonNode data = profileResponse.path("data");
    String externalAccountId = text(data, "id");
    return new OAuthCallbackResult(
        new TokenBundle(
            accessToken,
            text(tokenResponse, "refresh_token"),
            expiresAtFromSeconds(tokenResponse.path("expires_in").asLong(0)),
            List.copyOf(request.scopes())),
        new AccountProfile(
            externalAccountId,
            externalAccountId,
            "profile",
            text(data, "name"),
            text(data, "username"),
            asMap(profileResponse)),
        List.copyOf(settings.getCapabilities()),
        new WebhookSubscriptionDescriptor(
            "x-" + externalAccountId,
            settings.getClientSecret(),
            "active",
            Instant.now(),
            Map.of("provider", "x")));
  }

  @Override
  public TokenRefreshResult refreshAccessToken(TokenRefreshCommand command) {
    MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
    payload.add("grant_type", "refresh_token");
    payload.add("refresh_token", command.refreshToken());
    payload.add("client_id", settings.getClientId());

    JsonNode tokenResponse = restClient.post()
        .uri(settings.getRefreshUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .header(HttpHeaders.AUTHORIZATION, basicAuth())
        .body(payload)
        .retrieve()
        .body(JsonNode.class);

    String refreshedToken = text(tokenResponse, "refresh_token");
    return new TokenRefreshResult(
        new TokenBundle(
            text(tokenResponse, "access_token"),
            refreshedToken == null ? command.refreshToken() : refreshedToken,
            expiresAtFromSeconds(tokenResponse.path("expires_in").asLong(0)),
            command.scopes()),
        command.metadata());
  }

  @Override
  public PublicationResult publish(PublicationCommand command) {
    Map<String, Object> payload = new java.util.LinkedHashMap<>();
    payload.put("text", command.message());
    if (!command.mediaUrls().isEmpty()) {
      payload.put("media", Map.of("media_ids", command.mediaUrls()));
    }
    if (StringUtils.hasText(command.replyToExternalPostId())) {
      payload.put("reply", Map.of("in_reply_to_tweet_id", command.replyToExternalPostId()));
    }

    JsonNode response = restClient.post()
        .uri(settings.getPublishUri())
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + command.accessToken())
        .body(payload)
        .retrieve()
        .body(JsonNode.class);

    String externalPostId = text(response.path("data"), "id");
    return new PublicationResult(
        externalPostId,
        "https://x.com/i/web/status/" + externalPostId,
        Instant.now(),
        asMap(response));
  }

  @Override
  public WebhookChallengeResponse handleWebhookChallenge(Map<String, String> queryParameters) {
    String crcToken = queryParameters.get("crc_token");
    if (!StringUtils.hasText(crcToken)) {
      throw new IllegalArgumentException("Missing X webhook crc_token");
    }
    return new WebhookChallengeResponse(
        true,
        MediaType.APPLICATION_JSON_VALUE,
        null,
        Map.of("response_token", "sha256=" + base64Hmac(crcToken, settings.getClientSecret())));
  }

  @Override
  public WebhookEventResult parseWebhookEvent(Map<String, String> headers, String rawBody) {
    String signature = headers.getOrDefault("x-twitter-webhooks-signature", headers.get("X-Twitter-Webhooks-Signature"));
    if (!StringUtils.hasText(signature) || !signature.equals("sha256=" + base64Hmac(rawBody == null ? "" : rawBody, settings.getClientSecret()))) {
      return new WebhookEventResult(false, "X webhook signature is invalid", List.of());
    }

    JsonNode payload = readTree(rawBody);
    List<NormalizedInboundEvent> events = new ArrayList<>();
    for (JsonNode tweetEvent : payload.path("tweet_create_events")) {
      String userId = text(tweetEvent.path("user"), "id");
      events.add(
          new NormalizedInboundEvent(
              userId,
              userId,
              text(tweetEvent, "id_str"),
              text(tweetEvent, "in_reply_to_status_id_str"),
              "comment",
              text(tweetEvent.path("user"), "screen_name"),
              asMap(tweetEvent),
              Instant.now()));
    }
    for (JsonNode directMessage : payload.path("direct_message_events")) {
      String recipientId = text(directMessage.path("message_create").path("target"), "recipient_id");
      events.add(
          new NormalizedInboundEvent(
              recipientId,
              recipientId,
              text(directMessage, "id"),
              text(directMessage, "id"),
              "dm",
              text(directMessage.path("message_create"), "sender_id"),
              asMap(directMessage),
              Instant.now()));
    }

    return new WebhookEventResult(true, null, events);
  }

  private Instant expiresAtFromSeconds(long expiresIn) {
    return expiresIn <= 0 ? null : Instant.now().plusSeconds(expiresIn);
  }

  private String basicAuth() {
    String basic = settings.getClientId() + ":" + settings.getClientSecret();
    return "Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
  }

  private String codeChallenge(String codeVerifier) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest((codeVerifier == null ? "" : codeVerifier).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to build PKCE challenge", exception);
    }
  }

  private String base64Hmac(String value, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to calculate X webhook signature", exception);
    }
  }

  private String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private JsonNode readTree(String rawBody) {
    try {
      return objectMapper.readTree(rawBody == null ? "{}" : rawBody);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Unable to parse X webhook payload", exception);
    }
  }

  private Map<String, Object> asMap(JsonNode node) {
    return objectMapper.convertValue(node == null ? objectMapper.createObjectNode() : node, MAP_TYPE);
  }

  private String text(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode()) {
      return null;
    }
    JsonNode resolved = node.path(fieldName);
    return resolved.isMissingNode() || resolved.isNull() ? null : resolved.asText();
  }
}
