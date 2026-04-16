package com.nexora.gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexora.gateway")
public class GatewayDownstreamServicesProperties {

  private List<DownstreamService> downstreamServices = new ArrayList<>();
  private Duration probeTimeout = Duration.ofSeconds(2);

  public List<DownstreamService> getDownstreamServices() {
    return downstreamServices;
  }

  public void setDownstreamServices(List<DownstreamService> downstreamServices) {
    this.downstreamServices = downstreamServices;
  }

  public Duration getProbeTimeout() {
    return probeTimeout;
  }

  public void setProbeTimeout(Duration probeTimeout) {
    this.probeTimeout = probeTimeout;
  }

  public static class DownstreamService {
    private String service;
    private String baseUrl;
    private String infoPath = "/api/v1/system/info";
    private String docsPath = "/api-docs";

    public String getService() {
      return service;
    }

    public void setService(String service) {
      this.service = service;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getInfoPath() {
      return infoPath;
    }

    public void setInfoPath(String infoPath) {
      this.infoPath = infoPath;
    }

    public String getDocsPath() {
      return docsPath;
    }

    public void setDocsPath(String docsPath) {
      this.docsPath = docsPath;
    }
  }
}
