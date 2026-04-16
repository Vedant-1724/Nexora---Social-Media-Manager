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

@Component
public class LinkedInSocialProviderAdapter implements SocialProviderAdapter {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private final SocialIntegrationProperties properties;
  private final ProviderSettings settings;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public LinkedInSocialProviderAdapter(
      SocialIntegrationProperties properties,
      RestClient socialRestClient,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.settings = properties.provider(SocialProvider.LINKEDIN);
    this.restClient = socialRestClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public SocialProvider provider() {
    return SocialProvider.LINKEDIN;
  }

  @Override
  public ProviderDescriptor descriptor() {
    return new ProviderDescriptor(
        SocialProvider.LINKEDIN,
        "LinkedIn",
        settings.isEnabled(),
        false,
        properties.webhookPathFor(SocialProvider.LINKEDIN),
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
        + "&scope=" + encode(String.join(" ", request.scopes()));
  }

  @Override
  public OAuthCallbackResult exchangeAuthorizationCode(AuthorizationCodeExchange request) {
    MultiValueMap<String, String> tokenPayload = new LinkedMultiValueMap<>();
    tokenPayload.add("grant_type", "authorization_code");
    tokenPayload.add("code", request.code());
    tokenPayload.add("redirect_uri", request.callbackUri());
    tokenPayload.add("client_id", settings.getClientId());
    tokenPayload.add("client_secret", settings.getClientSecret());

    JsonNode tokenResponse = restClient.post()
        .uri(settings.getTokenUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(tokenPayload)
        .retrieve()
        .body(JsonNode.class);

    String accessToken = text(tokenResponse, "access_token");
    JsonNode profileResponse = restClient.get()
        .uri(settings.getProfileUri())
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .body(JsonNode.class);

    String externalAccountId = text(profileResponse, "id");
    String displayName =
        StringUtils.hasText(text(profileResponse, "localizedFirstName"))
            ? text(profileResponse, "localizedFirstName") + " " + text(profileResponse, "localizedLastName")
            : text(profileResponse, "name");

    return new OAuthCallbackResult(
        new TokenBundle(
            accessToken,
            text(tokenResponse, "refresh_token"),
            expiresAtFromSeconds(tokenResponse.path("expires_in").asLong(0)),
            List.copyOf(request.scopes())),
        new AccountProfile(
            externalAccountId,
            externalAccountId,
            "organization",
            displayName,
            text(profileResponse, "vanityName"),
            asMap(profileResponse)),
        List.copyOf(settings.getCapabilities()),
        new WebhookSubscriptionDescriptor(
            "linkedin-" + externalAccountId,
            settings.getClientSecret(),
            "active",
            Instant.now(),
            Map.of("provider", "linkedin")));
  }

  @Override
  public TokenRefreshResult refreshAccessToken(TokenRefreshCommand command) {
    MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
    payload.add("grant_type", "refresh_token");
    payload.add("refresh_token", command.refreshToken());
    payload.add("client_id", settings.getClientId());
    payload.add("client_secret", settings.getClientSecret());

    JsonNode tokenResponse = restClient.post()
        .uri(settings.getRefreshUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
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
    payload.put("author", "urn:li:person:" + command.externalAccountId());
    payload.put("lifecycleState", "PUBLISHED");
    payload.put(
        "specificContent",
        Map.of(
            "com.linkedin.ugc.ShareContent",
            Map.of(
                "shareCommentary", Map.of("text", command.message()),
                "shareMediaCategory", command.mediaUrls().isEmpty() ? "NONE" : "IMAGE")));
    payload.put("visibility", Map.of("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC"));

    JsonNode response = restClient.post()
        .uri(settings.getPublishUri())
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + command.accessToken())
        .body(payload)
        .retrieve()
        .body(JsonNode.class);

    String externalPostId = text(response, "id");
    return new PublicationResult(
        externalPostId,
        "https://www.linkedin.com/feed/update/" + externalPostId,
        Instant.now(),
        asMap(response));
  }

  @Override
  public WebhookChallengeResponse handleWebhookChallenge(Map<String, String> queryParameters) {
    return new WebhookChallengeResponse(false, null, null, Map.of());
  }

  @Override
  public WebhookEventResult parseWebhookEvent(Map<String, String> headers, String rawBody) {
    String signature = headers.getOrDefault("x-li-signature", headers.get("X-Li-Signature"));
    if (!verifyBase64Hmac(signature, rawBody, settings.getClientSecret())) {
      return new WebhookEventResult(false, "LinkedIn webhook signature is invalid", List.of());
    }

    JsonNode payload = readTree(rawBody);
    List<NormalizedInboundEvent> events = new ArrayList<>();
    for (JsonNode element : payload.path("elements")) {
      String organizationId = text(element, "organizationId");
      events.add(
          new NormalizedInboundEvent(
              organizationId,
              organizationId,
              stableEventId(organizationId, element),
              text(element, "threadId"),
              text(element, "eventType") == null ? "comment" : text(element, "eventType"),
              text(element, "actor"),
              asMap(element),
              Instant.now()));
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
      throw new IllegalArgumentException("Unable to parse LinkedIn webhook payload", exception);
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

  private String stableEventId(String organizationId, JsonNode payload) {
    return organizationId + "-" + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
  }

  private boolean verifyBase64Hmac(String signatureHeader, String body, String secret) {
    if (!StringUtils.hasText(signatureHeader)) {
      return false;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String expected = Base64.getEncoder().encodeToString(mac.doFinal((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)));
      return expected.equals(signatureHeader);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to verify LinkedIn webhook signature", exception);
    }
  }
}
