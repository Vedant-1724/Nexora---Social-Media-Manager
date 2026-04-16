package com.nexora.social;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.social.config.SocialIntegrationProperties;
import com.nexora.social.provider.LinkedInSocialProviderAdapter;
import com.nexora.social.provider.MetaSocialProviderAdapter;
import com.nexora.social.provider.SocialProvider;
import com.nexora.social.provider.SocialProviderAdapter;
import com.nexora.social.provider.XSocialProviderAdapter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SocialProviderAdapterContractTests {

  private MetaSocialProviderAdapter metaAdapter;
  private LinkedInSocialProviderAdapter linkedInAdapter;
  private XSocialProviderAdapter xAdapter;

  @BeforeEach
  void setUp() {
    SocialIntegrationProperties properties = new SocialIntegrationProperties();
    properties.getProviders().getMeta().setClientId("meta-client");
    properties.getProviders().getLinkedin().setClientId("linkedin-client");
    properties.getProviders().getX().setClientId("x-client");
    properties.getProviders().getX().setClientSecret("x-secret");
    RestClient restClient = RestClient.builder().build();
    ObjectMapper objectMapper = new ObjectMapper();

    metaAdapter = new MetaSocialProviderAdapter(properties, restClient, objectMapper);
    linkedInAdapter = new LinkedInSocialProviderAdapter(properties, restClient, objectMapper);
    xAdapter = new XSocialProviderAdapter(properties, restClient, objectMapper);
  }

  @Test
  void descriptorsExposeCapabilitiesAndRoadmapHints() {
    assertDescriptor(metaAdapter, SocialProvider.META, "publish.text");
    assertDescriptor(linkedInAdapter, SocialProvider.LINKEDIN, "inbox.comments");
    assertDescriptor(xAdapter, SocialProvider.X, "inbox.messages");
  }

  @Test
  void authorizationUrlsContainProviderSpecificRequirements() {
    assertThat(
            metaAdapter.buildAuthorizationUrl(
                new SocialProviderAdapter.AuthorizationRequest(
                    "state-123",
                    "http://localhost:18080/api/v1/social/oauth/meta/callback",
                    List.of("scope.one", "scope.two"),
                    null)))
        .contains("state=state-123")
        .contains("scope=scope.one%2Cscope.two");

    assertThat(
            linkedInAdapter.buildAuthorizationUrl(
                new SocialProviderAdapter.AuthorizationRequest(
                    "state-123",
                    "http://localhost:18080/api/v1/social/oauth/linkedin/callback",
                    List.of("scope.one", "scope.two"),
                    null)))
        .contains("state=state-123")
        .contains("scope=scope.one+scope.two");

    assertThat(
            xAdapter.buildAuthorizationUrl(
                new SocialProviderAdapter.AuthorizationRequest(
                    "state-123",
                    "http://localhost:18080/api/v1/social/oauth/x/callback",
                    List.of("scope.one", "scope.two"),
                    "code-verifier-123")))
        .contains("state=state-123")
        .contains("code_challenge_method=S256");
  }

  private void assertDescriptor(
      SocialProviderAdapter adapter,
      SocialProvider provider,
      String requiredCapability) {
    SocialProviderAdapter.ProviderDescriptor descriptor = adapter.descriptor();
    assertThat(descriptor.provider()).isEqualTo(provider);
    assertThat(descriptor.capabilities()).contains(requiredCapability);
    assertThat(descriptor.futureProviderHints()).contains("youtube", "tiktok", "pinterest");
  }
}
