package com.nexora.social;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.social.config.SocialIntegrationProperties;
import com.nexora.social.provider.LinkedInSocialProviderAdapter;
import com.nexora.social.provider.MetaSocialProviderAdapter;
import com.nexora.social.provider.SocialProviderAdapter;
import com.nexora.social.provider.XSocialProviderAdapter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SocialWebhookSignatureTests {

  private MetaSocialProviderAdapter metaAdapter;
  private LinkedInSocialProviderAdapter linkedInAdapter;
  private XSocialProviderAdapter xAdapter;

  @BeforeEach
  void setUp() {
    SocialIntegrationProperties properties = new SocialIntegrationProperties();
    properties.getProviders().getMeta().setClientSecret("meta-secret");
    properties.getProviders().getMeta().setWebhookVerifyToken("meta-verify-token");
    properties.getProviders().getLinkedin().setClientSecret("linkedin-secret");
    properties.getProviders().getX().setClientSecret("x-secret");
    RestClient restClient = RestClient.builder().build();
    ObjectMapper objectMapper = new ObjectMapper();

    metaAdapter = new MetaSocialProviderAdapter(properties, restClient, objectMapper);
    linkedInAdapter = new LinkedInSocialProviderAdapter(properties, restClient, objectMapper);
    xAdapter = new XSocialProviderAdapter(properties, restClient, objectMapper);
  }

  @Test
  void metaChallengeReturnsHubChallenge() {
    SocialProviderAdapter.WebhookChallengeResponse response =
        metaAdapter.handleWebhookChallenge(
            Map.of(
                "hub.mode", "subscribe",
                "hub.verify_token", "meta-verify-token",
                "hub.challenge", "challenge-123"));

    assertThat(response.handled()).isTrue();
    assertThat(response.plainTextBody()).isEqualTo("challenge-123");
  }

  @Test
  void metaWebhookSignatureValidationAcceptsValidPayload() {
    String body = "{\"entry\":[{\"id\":\"page-1\",\"changes\":[{\"value\":{\"item\":\"comment\",\"from\":{\"name\":\"lead\"}}}]}]}";
    String signature = "sha256=" + hexHmac(body, "meta-secret");

    SocialProviderAdapter.WebhookEventResult result =
        metaAdapter.parseWebhookEvent(Map.of("x-hub-signature-256", signature), body);

    assertThat(result.accepted()).isTrue();
    assertThat(result.events()).hasSize(1);
    assertThat(result.events().get(0).eventType()).isEqualTo("comment");
  }

  @Test
  void linkedInWebhookSignatureValidationAcceptsValidPayload() {
    String body = "{\"elements\":[{\"organizationId\":\"org-1\",\"eventType\":\"comment\",\"actor\":\"member-1\"}]}";
    String signature = base64Hmac(body, "linkedin-secret");

    SocialProviderAdapter.WebhookEventResult result =
        linkedInAdapter.parseWebhookEvent(Map.of("x-li-signature", signature), body);

    assertThat(result.accepted()).isTrue();
    assertThat(result.events()).hasSize(1);
    assertThat(result.events().get(0).externalAccountId()).isEqualTo("org-1");
  }

  @Test
  void xWebhookCrcAndSignatureValidationWork() {
    SocialProviderAdapter.WebhookChallengeResponse challengeResponse =
        xAdapter.handleWebhookChallenge(Map.of("crc_token", "crc-123"));
    assertThat(challengeResponse.jsonBody())
        .containsEntry("response_token", "sha256=" + base64Hmac("crc-123", "x-secret"));

    String body =
        "{\"tweet_create_events\":[{\"id_str\":\"tweet-1\",\"user\":{\"id\":\"user-1\",\"screen_name\":\"nexora\"}}]}";
    String signature = "sha256=" + base64Hmac(body, "x-secret");

    SocialProviderAdapter.WebhookEventResult result =
        xAdapter.parseWebhookEvent(Map.of("x-twitter-webhooks-signature", signature), body);

    assertThat(result.accepted()).isTrue();
    assertThat(result.events()).hasSize(1);
    assertThat(result.events().get(0).externalEventId()).isEqualTo("tweet-1");
  }

  private String hexHmac(String value, String secret) {
    byte[] bytes = hmac(value, secret);
    StringBuilder builder = new StringBuilder();
    for (byte current : bytes) {
      builder.append(String.format("%02x", current));
    }
    return builder.toString();
  }

  private String base64Hmac(String value, String secret) {
    return Base64.getEncoder().encodeToString(hmac(value, secret));
  }

  private byte[] hmac(String value, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }
}
