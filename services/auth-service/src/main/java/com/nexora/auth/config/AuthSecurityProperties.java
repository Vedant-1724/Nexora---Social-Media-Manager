package com.nexora.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexora.auth")
public class AuthSecurityProperties {

  private Jwt jwt = new Jwt();
  private Challenge challenge = new Challenge();
  private String userServiceBaseUrl = "http://localhost:8082";
  private boolean exposeDevelopmentTokens = true;

  public Jwt getJwt() {
    return jwt;
  }

  public void setJwt(Jwt jwt) {
    this.jwt = jwt;
  }

  public Challenge getChallenge() {
    return challenge;
  }

  public void setChallenge(Challenge challenge) {
    this.challenge = challenge;
  }

  public String getUserServiceBaseUrl() {
    return userServiceBaseUrl;
  }

  public void setUserServiceBaseUrl(String userServiceBaseUrl) {
    this.userServiceBaseUrl = userServiceBaseUrl;
  }

  public boolean isExposeDevelopmentTokens() {
    return exposeDevelopmentTokens;
  }

  public void setExposeDevelopmentTokens(boolean exposeDevelopmentTokens) {
    this.exposeDevelopmentTokens = exposeDevelopmentTokens;
  }

  public static class Jwt {
    private String issuer = "nexora-auth";
    private String secret = "nexora-dev-secret-change-me-please";
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(30);

    public String getIssuer() {
      return issuer;
    }

    public void setIssuer(String issuer) {
      this.issuer = issuer;
    }

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public Duration getAccessTokenTtl() {
      return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
      this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
      return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
      this.refreshTokenTtl = refreshTokenTtl;
    }
  }

  public static class Challenge {
    private Duration emailVerificationTtl = Duration.ofDays(2);
    private Duration passwordResetTtl = Duration.ofHours(2);

    public Duration getEmailVerificationTtl() {
      return emailVerificationTtl;
    }

    public void setEmailVerificationTtl(Duration emailVerificationTtl) {
      this.emailVerificationTtl = emailVerificationTtl;
    }

    public Duration getPasswordResetTtl() {
      return passwordResetTtl;
    }

    public void setPasswordResetTtl(Duration passwordResetTtl) {
      this.passwordResetTtl = passwordResetTtl;
    }
  }
}
