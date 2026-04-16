package com.nexora.scheduler.service;

import com.nexora.platform.core.web.NexoraHeaderNames;
import com.nexora.scheduler.config.PostSchedulerProperties;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SocialPublishingClient implements SocialPublishingGateway {

  private final RestClient restClient;
  private final PostSchedulerProperties properties;

  public SocialPublishingClient(RestClient schedulerRestClient, PostSchedulerProperties properties) {
    this.restClient = schedulerRestClient;
    this.properties = properties;
  }

  @Override
  public PublishResult publish(PublishCommand command) {
    return restClient.post()
        .uri(
            properties.getSocial().getBaseUrl()
                + "/api/v1/workspaces/{workspaceId}/social/publications",
            command.workspaceId())
        .headers(
            headers -> {
              headers.set(NexoraHeaderNames.USER_ID, command.actorUserId().toString());
              headers.set(NexoraHeaderNames.WORKSPACE_ID, command.workspaceId().toString());
              headers.set(NexoraHeaderNames.SCOPES, "posts.create");
            })
        .body(
            Map.of(
                "connectedAccountId", command.connectedAccountId(),
                "message", command.message(),
                "linkUrl", command.linkUrl(),
                "mediaUrls", command.mediaUrls(),
                "replyToExternalPostId", command.replyToExternalPostId(),
                "metadata", command.metadata()))
        .retrieve()
        .body(PublishResult.class);
  }
}
