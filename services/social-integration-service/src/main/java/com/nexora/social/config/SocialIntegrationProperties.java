package com.nexora.social.config;

import com.nexora.social.provider.SocialProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexora.social")
public class SocialIntegrationProperties {

  private OAuth oauth = new OAuth();
  private Encryption encryption = new Encryption();
  private Providers providers = new Providers();

  public OAuth getOauth() {
    return oauth;
  }

  public void setOauth(OAuth oauth) {
    this.oauth = oauth;
  }

  public Encryption getEncryption() {
    return encryption;
  }

  public void setEncryption(Encryption encryption) {
    this.encryption = encryption;
  }

  public Providers getProviders() {
    return providers;
  }

  public void setProviders(Providers providers) {
    this.providers = providers;
  }

  public ProviderSettings provider(SocialProvider provider) {
    return switch (provider) {
      case META -> providers.getMeta();
      case LINKEDIN -> providers.getLinkedin();
      case X -> providers.getX();
    };
  }

  public String callbackUrlFor(SocialProvider provider) {
    String baseUrl = oauth.getCallbackBaseUrl();
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl + "/api/v1/social/oauth/" + provider.code() + "/callback";
  }

  public String webhookPathFor(SocialProvider provider) {
    return "/api/v1/social/webhooks/" + provider.code();
  }

  public static class OAuth {
    private String callbackBaseUrl = "http://localhost:18080";
    private Duration stateTtl = Duration.ofMinutes(15);
    private Duration tokenRefreshSkew = Duration.ofMinutes(5);

    public String getCallbackBaseUrl() {
      return callbackBaseUrl;
    }

    public void setCallbackBaseUrl(String callbackBaseUrl) {
      this.callbackBaseUrl = callbackBaseUrl;
    }

    public Duration getStateTtl() {
      return stateTtl;
    }

    public void setStateTtl(Duration stateTtl) {
      this.stateTtl = stateTtl;
    }

    public Duration getTokenRefreshSkew() {
      return tokenRefreshSkew;
    }

    public void setTokenRefreshSkew(Duration tokenRefreshSkew) {
      this.tokenRefreshSkew = tokenRefreshSkew;
    }
  }

  public static class Encryption {
    private String activeKeyId = "dev-key";
    private String activeSecret = "nexora-social-dev-encryption-key-1234567890123456";

    public String getActiveKeyId() {
      return activeKeyId;
    }

    public void setActiveKeyId(String activeKeyId) {
      this.activeKeyId = activeKeyId;
    }

    public String getActiveSecret() {
      return activeSecret;
    }

    public void setActiveSecret(String activeSecret) {
      this.activeSecret = activeSecret;
    }
  }

  public static class Providers {
    private ProviderSettings meta = defaults(
        List.of("pages_show_list", "pages_manage_posts", "pages_read_engagement", "pages_manage_metadata"),
        List.of("publish.text", "publish.image", "inbox.comments", "inbox.messages"));
    private ProviderSettings linkedin = defaults(
        List.of("r_liteprofile", "r_emailaddress", "w_member_social"),
        List.of("publish.text", "publish.image", "inbox.comments"));
    private ProviderSettings x = defaults(
        List.of("tweet.read", "tweet.write", "users.read", "offline.access", "dm.read"),
        List.of("publish.text", "publish.image", "inbox.mentions", "inbox.messages"));

    public ProviderSettings getMeta() {
      return meta;
    }

    public void setMeta(ProviderSettings meta) {
      this.meta = meta;
    }

    public ProviderSettings getLinkedin() {
      return linkedin;
    }

    public void setLinkedin(ProviderSettings linkedin) {
      this.linkedin = linkedin;
    }

    public ProviderSettings getX() {
      return x;
    }

    public void setX(ProviderSettings x) {
      this.x = x;
    }

    private static ProviderSettings defaults(List<String> scopes, List<String> capabilities) {
      ProviderSettings settings = new ProviderSettings();
      settings.setDefaultScopes(scopes);
      settings.setCapabilities(capabilities);
      return settings;
    }
  }

  public static class ProviderSettings {
    private boolean enabled = true;
    private String clientId = "";
    private String clientSecret = "";
    private String authorizationUri = "";
    private String tokenUri = "";
    private String profileUri = "";
    private String publishUri = "";
    private String refreshUri = "";
    private String webhookVerifyToken = "";
    private List<String> defaultScopes = new ArrayList<>();
    private List<String> capabilities = new ArrayList<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
    }

    public String getAuthorizationUri() {
      return authorizationUri;
    }

    public void setAuthorizationUri(String authorizationUri) {
      this.authorizationUri = authorizationUri;
    }

    public String getTokenUri() {
      return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
      this.tokenUri = tokenUri;
    }

    public String getProfileUri() {
      return profileUri;
    }

    public void setProfileUri(String profileUri) {
      this.profileUri = profileUri;
    }

    public String getPublishUri() {
      return publishUri;
    }

    public void setPublishUri(String publishUri) {
      this.publishUri = publishUri;
    }

    public String getRefreshUri() {
      return refreshUri;
    }

    public void setRefreshUri(String refreshUri) {
      this.refreshUri = refreshUri;
    }

    public String getWebhookVerifyToken() {
      return webhookVerifyToken;
    }

    public void setWebhookVerifyToken(String webhookVerifyToken) {
      this.webhookVerifyToken = webhookVerifyToken;
    }

    public List<String> getDefaultScopes() {
      return defaultScopes;
    }

    public void setDefaultScopes(List<String> defaultScopes) {
      this.defaultScopes = defaultScopes == null ? new ArrayList<>() : new ArrayList<>(defaultScopes);
    }

    public List<String> getCapabilities() {
      return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
      this.capabilities = capabilities == null ? new ArrayList<>() : new ArrayList<>(capabilities);
    }
  }
}
