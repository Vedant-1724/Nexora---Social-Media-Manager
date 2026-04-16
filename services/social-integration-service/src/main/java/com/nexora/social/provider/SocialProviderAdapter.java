package com.nexora.social.provider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface SocialProviderAdapter {

  SocialProvider provider();

  ProviderDescriptor descriptor();

  String buildAuthorizationUrl(AuthorizationRequest request);

  OAuthCallbackResult exchangeAuthorizationCode(AuthorizationCodeExchange request);

  TokenRefreshResult refreshAccessToken(TokenRefreshCommand command);

  PublicationResult publish(PublicationCommand command);

  WebhookChallengeResponse handleWebhookChallenge(Map<String, String> queryParameters);

  WebhookEventResult parseWebhookEvent(Map<String, String> headers, String rawBody);

  record ProviderDescriptor(
      SocialProvider provider,
      String displayName,
      boolean enabled,
      boolean pkceRequired,
      String callbackPath,
      List<String> defaultScopes,
      List<String> capabilities,
      List<String> futureProviderHints) {}

  record AuthorizationRequest(
      String state,
      String callbackUri,
      List<String> scopes,
      String codeVerifier) {}

  record AuthorizationCodeExchange(
      String code,
      String callbackUri,
      List<String> scopes,
      String codeVerifier) {}

  record TokenBundle(
      String accessToken,
      String refreshToken,
      Instant expiresAt,
      List<String> scopes) {}

  record AccountProfile(
      String externalAccountId,
      String externalOrganizationId,
      String accountType,
      String displayName,
      String username,
      Map<String, Object> metadata) {}

  record WebhookSubscriptionDescriptor(
      String providerSubscriptionId,
      String signingSecret,
      String status,
      Instant subscribedAt,
      Map<String, Object> metadata) {}

  record OAuthCallbackResult(
      TokenBundle tokens,
      AccountProfile accountProfile,
      List<String> capabilities,
      WebhookSubscriptionDescriptor webhookSubscription) {}

  record TokenRefreshCommand(
      String accessToken,
      String refreshToken,
      String externalAccountId,
      String externalOrganizationId,
      List<String> scopes,
      Map<String, Object> metadata) {}

  record TokenRefreshResult(
      TokenBundle tokens,
      Map<String, Object> metadata) {}

  record PublicationCommand(
      String accessToken,
      String externalAccountId,
      String externalOrganizationId,
      String message,
      String linkUrl,
      List<String> mediaUrls,
      String replyToExternalPostId,
      Map<String, Object> metadata) {}

  record PublicationResult(
      String externalPostId,
      String providerPermalink,
      Instant publishedAt,
      Map<String, Object> rawResponse) {}

  record WebhookChallengeResponse(
      boolean handled,
      String contentType,
      String plainTextBody,
      Map<String, Object> jsonBody) {}

  record NormalizedInboundEvent(
      String externalAccountId,
      String externalOrganizationId,
      String externalEventId,
      String externalThreadId,
      String eventType,
      String actorHandle,
      Map<String, Object> payload,
      Instant occurredAt) {}

  record WebhookEventResult(
      boolean accepted,
      String rejectionReason,
      List<NormalizedInboundEvent> events) {}
}
