package com.nexora.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.platform.core.messaging.NexoraEventPublisher;
import com.nexora.social.config.SocialIntegrationProperties;
import com.nexora.social.provider.SocialProvider;
import com.nexora.social.provider.SocialProviderAdapter;
import com.nexora.social.provider.SocialProviderAdapterRegistry;
import com.nexora.social.repository.SocialIntegrationRepository;
import com.nexora.social.repository.SocialIntegrationRepository.ConnectedAccountRecord;
import com.nexora.social.repository.SocialIntegrationRepository.OAuthLinkStateRecord;
import com.nexora.social.service.SocialIntegrationService;
import com.nexora.social.service.TokenEncryptionService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class SocialIntegrationWorkflowServiceTests {

  @Mock
  private SocialIntegrationRepository repository;

  @Mock
  private SocialProviderAdapterRegistry adapterRegistry;

  @Mock
  private SocialProviderAdapter adapter;

  @Mock
  private ObjectProvider<NexoraEventPublisher> eventPublisherProvider;

  private TokenEncryptionService tokenEncryptionService;
  private SocialIntegrationService socialIntegrationService;
  private Clock clock;
  private SocialIntegrationProperties properties;

  @BeforeEach
  void setUp() {
    properties = new SocialIntegrationProperties();
    properties.getEncryption().setActiveSecret("nexora-social-dev-encryption-key-1234567890123456");
    clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC);
    tokenEncryptionService = new TokenEncryptionService(properties);
    socialIntegrationService =
        new SocialIntegrationService(
            repository,
            adapterRegistry,
            tokenEncryptionService,
            properties,
            clock,
            eventPublisherProvider);
  }

  @Test
  void createLinkSessionPersistsStateAndReturnsAuthorizationUrl() {
    when(adapterRegistry.require(SocialProvider.META)).thenReturn(adapter);
    when(adapter.descriptor()).thenReturn(
        new SocialProviderAdapter.ProviderDescriptor(
            SocialProvider.META,
            "Meta",
            true,
            false,
            "/api/v1/social/webhooks/meta",
            List.of("pages_manage_posts"),
            List.of("publish.text"),
            List.of("youtube", "tiktok", "pinterest")));
    when(adapter.buildAuthorizationUrl(any())).thenReturn("https://meta.example/oauth");

    SocialIntegrationService.LinkSessionView result =
        socialIntegrationService.createLinkSession(
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            SocialProvider.META,
            List.of("pages_manage_posts"));

    assertThat(result.authorizationUrl()).isEqualTo("https://meta.example/oauth");
    assertThat(result.provider()).isEqualTo("meta");
    verify(repository).insertOAuthLinkState(any(), any(), anyString(), any(), anyString(), any(), any(), any());
  }

  @Test
  void completeOAuthCallbackStoresEncryptedTokensAndCapabilities() {
    UUID workspaceId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    AtomicReference<UUID> insertedAccountId = new AtomicReference<>();

    when(repository.findOAuthLinkStateByHash(anyString()))
        .thenReturn(
            Optional.of(
                new OAuthLinkStateRecord(
                    UUID.fromString("70000000-0000-0000-0000-000000000001"),
                    workspaceId,
                    "meta",
                    userId,
                    "state-hash",
                    null,
                    List.of("pages_manage_posts"),
                    Instant.now(clock).plusSeconds(300),
                    null,
                    Instant.now(clock))));
    when(adapterRegistry.require(SocialProvider.META)).thenReturn(adapter);
    when(repository.findConnectedAccountByProviderExternalId("meta", "meta-account-1")).thenReturn(Optional.empty());
    when(adapter.exchangeAuthorizationCode(any())).thenReturn(
        new SocialProviderAdapter.OAuthCallbackResult(
            new SocialProviderAdapter.TokenBundle(
                "raw-access-token",
                "raw-refresh-token",
                Instant.now(clock).plusSeconds(3600),
                List.of("pages_manage_posts")),
            new SocialProviderAdapter.AccountProfile(
                "meta-account-1",
                "meta-account-1",
                "page",
                "Northstar Meta",
                "northstarmeta",
                Map.of("provider", "meta")),
            List.of("publish.text", "inbox.comments"),
            new SocialProviderAdapter.WebhookSubscriptionDescriptor(
                "meta-subscription-1",
                "meta-signing-secret",
                "active",
                Instant.now(clock),
                Map.of("provider", "meta"))));
    when(repository.findWebhookSubscription(any())).thenReturn(Optional.empty());
    org.mockito.Mockito.doAnswer(
            invocation -> {
              insertedAccountId.set(invocation.getArgument(0));
              return null;
            })
        .when(repository)
        .insertConnectedAccount(
            any(),
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any());
    when(repository.findConnectedAccount(org.mockito.ArgumentMatchers.eq(workspaceId), any()))
        .thenAnswer(
            invocation ->
                Optional.of(
                    new ConnectedAccountRecord(
                        insertedAccountId.get(),
                        workspaceId,
                        "meta",
                        "meta-account-1",
                        "meta-account-1",
                        "page",
                        "Northstar Meta",
                        "northstarmeta",
                        "active",
                        tokenEncryptionService.encrypt("raw-access-token"),
                        tokenEncryptionService.encrypt("raw-refresh-token"),
                        tokenEncryptionService.activeKeyId(),
                        Instant.now(clock).plusSeconds(3600),
                        Instant.now(clock),
                        List.of("pages_manage_posts"),
                        userId,
                        Map.of("provider", "meta"),
                        Instant.now(clock),
                        Instant.now(clock),
                        Instant.now(clock))));
    when(repository.findCapabilities(any())).thenReturn(List.of("publish.text", "inbox.comments"));

    SocialIntegrationService.OAuthCallbackView result =
        socialIntegrationService.completeOAuthCallback(SocialProvider.META, "auth-code", "state-token");

    assertThat(result.status()).isEqualTo("linked");
    assertThat(result.account().provider()).isEqualTo("meta");
    assertThat(result.account().capabilities()).contains("publish.text");
    verify(repository).replaceCapabilities(any(), any());
    verify(repository).insertWebhookSubscription(any(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), any());
    verify(repository).markOAuthLinkStateConsumed(any(), any());

    ArgumentCaptor<String> accessTokenCaptor = ArgumentCaptor.forClass(String.class);
    verify(repository).insertConnectedAccount(
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        accessTokenCaptor.capture(),
        anyString(),
        anyString(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any());
    assertThat(accessTokenCaptor.getValue()).startsWith("enc:v1:");
    assertThat(tokenEncryptionService.decrypt(accessTokenCaptor.getValue())).isEqualTo("raw-access-token");
  }

  @Test
  void publishRefreshesExpiredTokensBeforeDelegating() {
    UUID workspaceId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID accountId = UUID.fromString("30000000-0000-0000-0000-000000000011");

    ConnectedAccountRecord expiringAccount =
        new ConnectedAccountRecord(
            accountId,
            workspaceId,
            "x",
            "x-account-1",
            "x-account-1",
            "profile",
            "Nexora X",
            "nexorax",
            "active",
            tokenEncryptionService.encrypt("old-access"),
            tokenEncryptionService.encrypt("old-refresh"),
            tokenEncryptionService.activeKeyId(),
            Instant.now(clock).minusSeconds(30),
            Instant.now(clock).minusSeconds(3600),
            List.of("tweet.write"),
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            Map.of("provider", "x"),
            Instant.now(clock).minusSeconds(3600),
            Instant.now(clock).minusSeconds(7200),
            Instant.now(clock).minusSeconds(3600));
    ConnectedAccountRecord refreshedAccount =
        new ConnectedAccountRecord(
            accountId,
            workspaceId,
            "x",
            "x-account-1",
            "x-account-1",
            "profile",
            "Nexora X",
            "nexorax",
            "active",
            tokenEncryptionService.encrypt("new-access"),
            tokenEncryptionService.encrypt("new-refresh"),
            tokenEncryptionService.activeKeyId(),
            Instant.now(clock).plusSeconds(3600),
            Instant.now(clock),
            List.of("tweet.write"),
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            Map.of("provider", "x"),
            Instant.now(clock),
            Instant.now(clock).minusSeconds(7200),
            Instant.now(clock));

    when(repository.findConnectedAccount(workspaceId, accountId))
        .thenReturn(Optional.of(expiringAccount), Optional.of(refreshedAccount));
    when(adapterRegistry.require(SocialProvider.X)).thenReturn(adapter);
    when(adapter.refreshAccessToken(any())).thenReturn(
        new SocialProviderAdapter.TokenRefreshResult(
            new SocialProviderAdapter.TokenBundle(
                "new-access",
                "new-refresh",
                Instant.now(clock).plusSeconds(3600),
                List.of("tweet.write")),
            Map.of("refreshed", true)));
    when(adapter.publish(any())).thenReturn(
        new SocialProviderAdapter.PublicationResult(
            "tweet-1",
            "https://x.com/i/web/status/tweet-1",
            Instant.now(clock),
            Map.of()));

    SocialIntegrationService.PublicationView result =
        socialIntegrationService.publish(
            workspaceId,
            new SocialIntegrationService.PublishCommand(
                accountId,
                "Launching Phase 5",
                null,
                List.of(),
                null,
                Map.of()));

    assertThat(result.externalPostId()).isEqualTo("tweet-1");
    verify(repository).updateConnectedAccount(
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        any(),
        any(),
        any(),
        any());
    ArgumentCaptor<SocialProviderAdapter.PublicationCommand> commandCaptor =
        ArgumentCaptor.forClass(SocialProviderAdapter.PublicationCommand.class);
    verify(adapter).publish(commandCaptor.capture());
    assertThat(commandCaptor.getValue().accessToken()).isEqualTo("new-access");
  }

  @Test
  void processWebhookEventStoresOnlyMatchedAndUniqueEvents() {
    UUID workspaceId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID accountId = UUID.fromString("30000000-0000-0000-0000-000000000011");
    ConnectedAccountRecord account =
        new ConnectedAccountRecord(
            accountId,
            workspaceId,
            "linkedin",
            "org-1",
            "org-1",
            "organization",
            "Northstar LinkedIn",
            "northstar",
            "active",
            tokenEncryptionService.encrypt("access"),
            tokenEncryptionService.encrypt("refresh"),
            tokenEncryptionService.activeKeyId(),
            Instant.now(clock).plusSeconds(3600),
            Instant.now(clock),
            List.of("w_member_social"),
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            Map.of(),
            Instant.now(clock),
            Instant.now(clock),
            Instant.now(clock));

    when(adapterRegistry.require(SocialProvider.LINKEDIN)).thenReturn(adapter);
    when(adapter.parseWebhookEvent(any(), anyString())).thenReturn(
        new SocialProviderAdapter.WebhookEventResult(
            true,
            null,
            List.of(
                new SocialProviderAdapter.NormalizedInboundEvent(
                    "org-1",
                    "org-1",
                    "evt-1",
                    "thread-1",
                    "comment",
                    "actor-1",
                    Map.of("message", "hello"),
                    Instant.now(clock)),
                new SocialProviderAdapter.NormalizedInboundEvent(
                    "org-1",
                    "org-1",
                    "evt-2",
                    "thread-2",
                    "comment",
                    "actor-2",
                    Map.of("message", "duplicate"),
                    Instant.now(clock)))));
    when(repository.findConnectedAccountByProviderExternalId("linkedin", "org-1"))
        .thenReturn(Optional.of(account));
    when(repository.insertInboundInboxEvent(any(), any(), any(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any()))
        .thenReturn(true, false);

    SocialIntegrationService.WebhookDeliveryView result =
        socialIntegrationService.processWebhookEvent(
            SocialProvider.LINKEDIN,
            Map.of("x-li-signature", "signature"),
            "{\"elements\":[]}");

    assertThat(result.receivedEvents()).isEqualTo(2);
    assertThat(result.acceptedEvents()).isEqualTo(1);
  }
}
