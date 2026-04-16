package com.nexora.social.service;

import com.nexora.platform.core.api.MessagePublishRequest;
import com.nexora.platform.core.messaging.NexoraEventPublisher;
import com.nexora.platform.core.security.TokenHashingUtils;
import com.nexora.social.config.SocialIntegrationProperties;
import com.nexora.social.provider.SocialProvider;
import com.nexora.social.provider.SocialProviderAdapter;
import com.nexora.social.provider.SocialProviderAdapter.AccountProfile;
import com.nexora.social.provider.SocialProviderAdapter.AuthorizationRequest;
import com.nexora.social.provider.SocialProviderAdapter.NormalizedInboundEvent;
import com.nexora.social.provider.SocialProviderAdapter.ProviderDescriptor;
import com.nexora.social.provider.SocialProviderAdapter.PublicationCommand;
import com.nexora.social.provider.SocialProviderAdapter.PublicationResult;
import com.nexora.social.provider.SocialProviderAdapter.TokenBundle;
import com.nexora.social.provider.SocialProviderAdapter.TokenRefreshCommand;
import com.nexora.social.provider.SocialProviderAdapter.WebhookChallengeResponse;
import com.nexora.social.provider.SocialProviderAdapter.WebhookEventResult;
import com.nexora.social.provider.SocialProviderAdapterRegistry;
import com.nexora.social.repository.SocialIntegrationRepository;
import com.nexora.social.repository.SocialIntegrationRepository.ConnectedAccountRecord;
import com.nexora.social.repository.SocialIntegrationRepository.OAuthLinkStateRecord;
import com.nexora.social.repository.SocialIntegrationRepository.WebhookSubscriptionRecord;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SocialIntegrationService {

  private final SocialIntegrationRepository repository;
  private final SocialProviderAdapterRegistry adapterRegistry;
  private final TokenEncryptionService tokenEncryptionService;
  private final SocialIntegrationProperties properties;
  private final Clock clock;
  private final NexoraEventPublisher eventPublisher;
  private final SecureRandom secureRandom = new SecureRandom();

  public SocialIntegrationService(
      SocialIntegrationRepository repository,
      SocialProviderAdapterRegistry adapterRegistry,
      TokenEncryptionService tokenEncryptionService,
      SocialIntegrationProperties properties,
      Clock clock,
      ObjectProvider<NexoraEventPublisher> eventPublisherProvider) {
    this.repository = repository;
    this.adapterRegistry = adapterRegistry;
    this.tokenEncryptionService = tokenEncryptionService;
    this.properties = properties;
    this.clock = clock;
    this.eventPublisher = eventPublisherProvider.getIfAvailable();
  }

  @Transactional(readOnly = true)
  public List<ProviderDescriptor> listProviders() {
    return adapterRegistry.descriptors();
  }

  @Transactional
  public LinkSessionView createLinkSession(
      UUID actorUserId,
      UUID workspaceId,
      SocialProvider provider,
      List<String> requestedScopes) {
    SocialProviderAdapter adapter = adapterRegistry.require(provider);
    String rawStateToken = opaqueToken(48);
    String codeVerifier = adapter.descriptor().pkceRequired() ? opaqueToken(64) : null;
    List<String> scopes =
        requestedScopes == null || requestedScopes.isEmpty()
            ? adapter.descriptor().defaultScopes()
            : requestedScopes;
    Instant expiresAt = Instant.now(clock).plus(properties.getOauth().getStateTtl());

    repository.insertOAuthLinkState(
        UUID.randomUUID(),
        workspaceId,
        provider.code(),
        actorUserId,
        TokenHashingUtils.sha256(rawStateToken),
        codeVerifier,
        scopes,
        expiresAt);

    String authorizationUrl =
        adapter.buildAuthorizationUrl(
            new AuthorizationRequest(
                rawStateToken,
                properties.callbackUrlFor(provider),
                scopes,
                codeVerifier));

    return new LinkSessionView(
        provider.code(),
        authorizationUrl,
        properties.callbackUrlFor(provider),
        expiresAt,
        scopes,
        adapter.descriptor().capabilities());
  }

  @Transactional(readOnly = true)
  public List<AccountView> listConnectedAccounts(UUID workspaceId) {
    return repository.findConnectedAccounts(workspaceId).stream()
        .map(this::toAccountView)
        .toList();
  }

  @Transactional
  public OAuthCallbackView completeOAuthCallback(SocialProvider provider, String code, String state) {
    OAuthLinkStateRecord linkState =
        repository.findOAuthLinkStateByHash(TokenHashingUtils.sha256(state))
            .orElseThrow(() -> new IllegalArgumentException("OAuth state token is invalid"));

    if (!provider.code().equals(linkState.provider())) {
      throw new IllegalArgumentException("OAuth callback provider does not match the linking session");
    }
    if (linkState.consumedAt() != null) {
      throw new IllegalArgumentException("OAuth state token has already been consumed");
    }
    if (linkState.expiresAt().isBefore(Instant.now(clock))) {
      throw new IllegalArgumentException("OAuth state token has expired");
    }

    SocialProviderAdapter adapter = adapterRegistry.require(provider);
    SocialProviderAdapter.OAuthCallbackResult callbackResult =
        adapter.exchangeAuthorizationCode(
            new SocialProviderAdapter.AuthorizationCodeExchange(
                code,
                properties.callbackUrlFor(provider),
                linkState.requestedScopes(),
                linkState.codeVerifier()));

    ConnectedAccountRecord existing =
        repository.findConnectedAccountByProviderExternalId(
                provider.code(),
                callbackResult.accountProfile().externalAccountId())
            .orElse(null);
    if (existing != null && !existing.workspaceId().equals(linkState.workspaceId())) {
      throw new IllegalArgumentException("This social account is already linked to another workspace");
    }

    UUID connectedAccountId = existing == null ? UUID.randomUUID() : existing.id();
    AccountProfile accountProfile = callbackResult.accountProfile();
    TokenBundle tokens = callbackResult.tokens();
    String encryptedAccessToken = tokenEncryptionService.encrypt(tokens.accessToken());
    String encryptedRefreshToken = tokenEncryptionService.encrypt(tokens.refreshToken());
    Instant now = Instant.now(clock);

    if (existing == null) {
      repository.insertConnectedAccount(
          connectedAccountId,
          linkState.workspaceId(),
          provider.code(),
          accountProfile.externalAccountId(),
          accountProfile.externalOrganizationId(),
          accountProfile.accountType(),
          accountProfile.displayName(),
          accountProfile.username(),
          "active",
          encryptedAccessToken,
          encryptedRefreshToken,
          tokenEncryptionService.activeKeyId(),
          tokens.expiresAt(),
          now,
          tokens.scopes(),
          linkState.connectedByUserId(),
          accountProfile.metadata(),
          now);
    } else {
      repository.updateConnectedAccount(
          existing.id(),
          accountProfile.externalOrganizationId(),
          accountProfile.accountType(),
          accountProfile.displayName(),
          accountProfile.username(),
          "active",
          encryptedAccessToken,
          encryptedRefreshToken,
          tokenEncryptionService.activeKeyId(),
          tokens.expiresAt(),
          now,
          tokens.scopes(),
          mergeMetadata(existing.metadata(), accountProfile.metadata()),
          now);
    }

    repository.replaceCapabilities(connectedAccountId, callbackResult.capabilities());
    upsertWebhookSubscription(provider, connectedAccountId, callbackResult.webhookSubscription(), now);
    repository.markOAuthLinkStateConsumed(linkState.id(), now);

    publishEvent(
        "SocialAccountConnected",
        Map.of(
            "workspaceId", linkState.workspaceId().toString(),
            "socialAccountId", connectedAccountId.toString(),
            "provider", provider.code(),
            "externalAccountId", accountProfile.externalAccountId(),
            "connectedByUserId", linkState.connectedByUserId().toString()));

    return new OAuthCallbackView(
        "linked",
        provider.code(),
        toAccountView(
            repository.findConnectedAccount(linkState.workspaceId(), connectedAccountId)
                .orElseThrow(() -> new IllegalStateException("Linked account could not be reloaded"))));
  }

  @Transactional
  public AccountView refreshAccountTokens(UUID workspaceId, UUID connectedAccountId) {
    ConnectedAccountRecord account = loadConnectedAccount(workspaceId, connectedAccountId);
    SocialProviderAdapter adapter = adapterRegistry.require(SocialProvider.fromCode(account.provider()));
    ConnectedAccountRecord refreshed = refreshTokens(account, adapter);
    return toAccountView(refreshed);
  }

  @Transactional
  public void revokeAccount(UUID workspaceId, UUID connectedAccountId) {
    loadConnectedAccount(workspaceId, connectedAccountId);
    repository.updateConnectedAccountStatus(connectedAccountId, "revoked");
  }

  @Transactional
  public PublicationView publish(UUID workspaceId, PublishCommand command) {
    ConnectedAccountRecord account = loadConnectedAccount(workspaceId, command.connectedAccountId());
    SocialProviderAdapter adapter = adapterRegistry.require(SocialProvider.fromCode(account.provider()));
    ConnectedAccountRecord accountForPublish = ensureFreshAccount(account, adapter);
    String accessToken = tokenEncryptionService.decrypt(accountForPublish.accessTokenCiphertext());

    PublicationResult result =
        adapter.publish(
            new PublicationCommand(
                accessToken,
                accountForPublish.externalAccountId(),
                accountForPublish.externalOrganizationId(),
                command.message(),
                command.linkUrl(),
                command.mediaUrls(),
                command.replyToExternalPostId(),
                command.metadata()));

    return new PublicationView(
        accountForPublish.id(),
        accountForPublish.provider(),
        result.externalPostId(),
        result.providerPermalink(),
        result.publishedAt());
  }

  @Transactional(readOnly = true)
  public WebhookChallengeResponse handleWebhookChallenge(
      SocialProvider provider,
      Map<String, String> queryParameters) {
    return adapterRegistry.require(provider).handleWebhookChallenge(queryParameters);
  }

  @Transactional
  public WebhookDeliveryView processWebhookEvent(
      SocialProvider provider,
      Map<String, String> headers,
      String rawBody) {
    SocialProviderAdapter adapter = adapterRegistry.require(provider);
    WebhookEventResult result = adapter.parseWebhookEvent(headers, rawBody);
    if (!result.accepted()) {
      throw new IllegalArgumentException(result.rejectionReason());
    }

    int acceptedEvents = 0;
    for (NormalizedInboundEvent event : result.events()) {
      ConnectedAccountRecord account = resolveWebhookAccount(provider, event);
      if (account == null) {
        continue;
      }

      boolean inserted =
          repository.insertInboundInboxEvent(
              UUID.randomUUID(),
              account.workspaceId(),
              account.id(),
              provider.code(),
              event.externalEventId(),
              event.externalThreadId(),
              normalizeEventType(event.eventType()),
              event.actorHandle(),
              event.payload(),
              event.occurredAt() == null ? Instant.now(clock) : event.occurredAt());
      if (inserted) {
        acceptedEvents++;
        publishEvent(
            "InboxEventIngested",
            Map.of(
                "workspaceId", account.workspaceId().toString(),
                "inboxEventId", event.externalEventId(),
                "provider", provider.code(),
                "messageType", normalizeEventType(event.eventType()),
                "receivedAt", Instant.now(clock).toString()));
      }
    }

    return new WebhookDeliveryView(provider.code(), result.events().size(), acceptedEvents, "accepted");
  }

  private ConnectedAccountRecord ensureFreshAccount(
      ConnectedAccountRecord account,
      SocialProviderAdapter adapter) {
    if (account.tokenExpiresAt() == null) {
      return account;
    }
    Instant refreshThreshold = Instant.now(clock).plus(properties.getOauth().getTokenRefreshSkew());
    if (account.tokenExpiresAt().isAfter(refreshThreshold)) {
      return account;
    }
    return refreshTokens(account, adapter);
  }

  private ConnectedAccountRecord refreshTokens(
      ConnectedAccountRecord account,
      SocialProviderAdapter adapter) {
    String refreshToken = tokenEncryptionService.decrypt(account.refreshTokenCiphertext());
    String accessToken = tokenEncryptionService.decrypt(account.accessTokenCiphertext());
    if (!StringUtils.hasText(accessToken)) {
      throw new IllegalStateException("Connected account is missing an access token");
    }

    SocialProviderAdapter.TokenRefreshResult refreshResult =
        adapter.refreshAccessToken(
            new TokenRefreshCommand(
                accessToken,
                refreshToken,
                account.externalAccountId(),
                account.externalOrganizationId(),
                account.scopes(),
                account.metadata()));

    repository.updateConnectedAccount(
        account.id(),
        account.externalOrganizationId(),
        account.providerAccountType(),
        account.displayName(),
        account.username(),
        "active",
        tokenEncryptionService.encrypt(refreshResult.tokens().accessToken()),
        tokenEncryptionService.encrypt(refreshResult.tokens().refreshToken()),
        tokenEncryptionService.activeKeyId(),
        refreshResult.tokens().expiresAt(),
        Instant.now(clock),
        refreshResult.tokens().scopes(),
        mergeMetadata(account.metadata(), refreshResult.metadata()),
        Instant.now(clock));

    return repository.findConnectedAccount(account.workspaceId(), account.id())
        .orElseThrow(
            () -> new IllegalStateException("Connected account could not be reloaded after refresh"));
  }

  private void upsertWebhookSubscription(
      SocialProvider provider,
      UUID connectedAccountId,
      SocialProviderAdapter.WebhookSubscriptionDescriptor webhookSubscription,
      Instant now) {
    if (webhookSubscription == null) {
      return;
    }
    WebhookSubscriptionRecord existing = repository.findWebhookSubscription(connectedAccountId).orElse(null);
    String encryptedSecret = tokenEncryptionService.encrypt(webhookSubscription.signingSecret());
    if (existing == null) {
      repository.insertWebhookSubscription(
          UUID.randomUUID(),
          connectedAccountId,
          webhookSubscription.providerSubscriptionId(),
          properties.webhookPathFor(provider),
          encryptedSecret,
          webhookSubscription.status(),
          Objects.requireNonNullElse(webhookSubscription.subscribedAt(), now),
          now,
          webhookSubscription.metadata());
    } else {
      repository.updateWebhookSubscription(
          existing.id(),
          webhookSubscription.providerSubscriptionId(),
          properties.webhookPathFor(provider),
          encryptedSecret,
          webhookSubscription.status(),
          Objects.requireNonNullElse(webhookSubscription.subscribedAt(), existing.subscribedAt()),
          now,
          mergeMetadata(existing.metadata(), webhookSubscription.metadata()));
    }
  }

  private ConnectedAccountRecord resolveWebhookAccount(
      SocialProvider provider,
      NormalizedInboundEvent event) {
    ConnectedAccountRecord account =
        repository.findConnectedAccountByProviderExternalId(
                provider.code(),
                event.externalAccountId())
            .orElse(null);
    if (account != null) {
      return account;
    }
    if (!StringUtils.hasText(event.externalOrganizationId())) {
      return null;
    }
    return repository.findConnectedAccountByProviderOrganizationId(
            provider.code(),
            event.externalOrganizationId())
        .orElse(null);
  }

  private ConnectedAccountRecord loadConnectedAccount(
      UUID workspaceId,
      UUID connectedAccountId) {
    return repository.findConnectedAccount(workspaceId, connectedAccountId)
        .orElseThrow(() -> new IllegalArgumentException("Connected social account was not found"));
  }

  private AccountView toAccountView(ConnectedAccountRecord account) {
    return new AccountView(
        account.id(),
        account.workspaceId(),
        account.provider(),
        account.externalAccountId(),
        account.externalOrganizationId(),
        account.providerAccountType(),
        account.displayName(),
        account.username(),
        account.status(),
        account.tokenExpiresAt(),
        account.tokenRefreshedAt(),
        account.scopes(),
        repository.findCapabilities(account.id()),
        account.lastSyncAt(),
        account.createdAt(),
        account.updatedAt());
  }

  private Map<String, Object> mergeMetadata(
      Map<String, Object> first,
      Map<String, Object> second) {
    Map<String, Object> merged = new LinkedHashMap<>();
    if (first != null) {
      merged.putAll(first);
    }
    if (second != null) {
      merged.putAll(second);
    }
    return merged;
  }

  private String normalizeEventType(String rawEventType) {
    if (!StringUtils.hasText(rawEventType)) {
      return "comment";
    }
    return switch (rawEventType.toLowerCase()) {
      case "mention" -> "mention";
      case "dm" -> "dm";
      default -> "comment";
    };
  }

  private void publishEvent(String type, Map<String, Object> payload) {
    if (eventPublisher == null) {
      return;
    }
    String workspaceId = Objects.toString(payload.get("workspaceId"), null);
    eventPublisher.publish(
        new MessagePublishRequest(type, null, null, payload),
        workspaceId == null ? "social-integration-service" : workspaceId);
  }

  private String opaqueToken(int byteLength) {
    byte[] bytes = new byte[byteLength];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public record LinkSessionView(
      String provider,
      String authorizationUrl,
      String callbackUrl,
      Instant expiresAt,
      List<String> scopes,
      List<String> capabilities) {}

  public record AccountView(
      UUID connectedAccountId,
      UUID workspaceId,
      String provider,
      String externalAccountId,
      String externalOrganizationId,
      String providerAccountType,
      String displayName,
      String username,
      String status,
      Instant tokenExpiresAt,
      Instant tokenRefreshedAt,
      List<String> scopes,
      List<String> capabilities,
      Instant lastSyncAt,
      Instant createdAt,
      Instant updatedAt) {}

  public record OAuthCallbackView(
      String status,
      String provider,
      AccountView account) {}

  public record PublishCommand(
      UUID connectedAccountId,
      String message,
      String linkUrl,
      List<String> mediaUrls,
      String replyToExternalPostId,
      Map<String, Object> metadata) {}

  public record PublicationView(
      UUID connectedAccountId,
      String provider,
      String externalPostId,
      String providerPermalink,
      Instant publishedAt) {}

  public record WebhookDeliveryView(
      String provider,
      int receivedEvents,
      int acceptedEvents,
      String status) {}
}
