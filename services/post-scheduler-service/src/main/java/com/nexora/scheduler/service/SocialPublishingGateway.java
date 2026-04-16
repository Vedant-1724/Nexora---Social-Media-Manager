package com.nexora.scheduler.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SocialPublishingGateway {

  PublishResult publish(PublishCommand command);

  record PublishCommand(
      UUID workspaceId,
      UUID actorUserId,
      UUID connectedAccountId,
      String message,
      String linkUrl,
      List<String> mediaUrls,
      String replyToExternalPostId,
      Map<String, Object> metadata) {}

  record PublishResult(
      UUID connectedAccountId,
      String provider,
      String externalPostId,
      String providerPermalink,
      Instant publishedAt) {}
}
