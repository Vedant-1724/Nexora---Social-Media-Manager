package com.nexora.platform.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexora.service")
public class NexoraServiceProperties {

  private String name = "nexora-service";
  private String displayName = "Nexora Service";
  private String description = "Nexora platform capability";
  private String phase = "phase-3";
  private String version = "0.1.0";
  private String environment = "local";
  private String docsPath = "/api-docs";

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getDocsPath() {
    return docsPath;
  }

  public void setDocsPath(String docsPath) {
    this.docsPath = docsPath;
  }
}
