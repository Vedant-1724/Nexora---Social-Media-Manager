package com.nexora.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nexora.scheduler")
public class PostSchedulerProperties {

  private final Social social = new Social();
  private final Dispatch dispatch = new Dispatch();

  public Social getSocial() {
    return social;
  }

  public Dispatch getDispatch() {
    return dispatch;
  }

  public static class Social {

    private String baseUrl = "http://localhost:8083";

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }
  }

  public static class Dispatch {

    private boolean enabled = true;
    private long pollIntervalMs = 15_000L;
    private int dueBatchSize = 10;
    private int maxRetries = 3;
    private long initialRetryDelayMs = 300_000L;
    private long maxRetryDelayMs = 3_600_000L;
    private double retryMultiplier = 2.0d;
    private String workerId = "post-scheduler-service";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public long getPollIntervalMs() {
      return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
      this.pollIntervalMs = pollIntervalMs;
    }

    public int getDueBatchSize() {
      return dueBatchSize;
    }

    public void setDueBatchSize(int dueBatchSize) {
      this.dueBatchSize = dueBatchSize;
    }

    public int getMaxRetries() {
      return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
    }

    public long getInitialRetryDelayMs() {
      return initialRetryDelayMs;
    }

    public void setInitialRetryDelayMs(long initialRetryDelayMs) {
      this.initialRetryDelayMs = initialRetryDelayMs;
    }

    public long getMaxRetryDelayMs() {
      return maxRetryDelayMs;
    }

    public void setMaxRetryDelayMs(long maxRetryDelayMs) {
      this.maxRetryDelayMs = maxRetryDelayMs;
    }

    public double getRetryMultiplier() {
      return retryMultiplier;
    }

    public void setRetryMultiplier(double retryMultiplier) {
      this.retryMultiplier = retryMultiplier;
    }

    public String getWorkerId() {
      return workerId;
    }

    public void setWorkerId(String workerId) {
      this.workerId = workerId;
    }
  }
}
