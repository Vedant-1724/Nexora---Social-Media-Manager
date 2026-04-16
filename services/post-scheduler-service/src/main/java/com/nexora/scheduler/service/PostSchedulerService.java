package com.nexora.scheduler.service;

import com.nexora.platform.core.api.MessagePublishRequest;
import com.nexora.platform.core.messaging.NexoraEventPublisher;
import com.nexora.scheduler.config.PostSchedulerProperties;
import com.nexora.scheduler.repository.PostSchedulerRepository;
import com.nexora.scheduler.repository.PostSchedulerRepository.ApprovalDecisionRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.ApprovalRequestRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.CalendarEntryRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.DispatchBundle;
import com.nexora.scheduler.repository.PostSchedulerRepository.DraftRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.DraftSummaryRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.MediaAssetRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.ScheduledJobRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.VariantRecord;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PostSchedulerService {

  private final PostSchedulerRepository repository;
  private final SocialPublishingGateway socialPublishingGateway;
  private final PostSchedulerProperties properties;
  private final Clock clock;
  private final NexoraEventPublisher eventPublisher;

  public PostSchedulerService(
      PostSchedulerRepository repository,
      SocialPublishingGateway socialPublishingGateway,
      PostSchedulerProperties properties,
      Clock clock,
      ObjectProvider<NexoraEventPublisher> eventPublisherProvider) {
    this.repository = repository;
    this.socialPublishingGateway = socialPublishingGateway;
    this.properties = properties;
    this.clock = clock;
    this.eventPublisher = eventPublisherProvider.getIfAvailable();
  }

  @Transactional(readOnly = true)
  public List<DraftSummaryView> listDrafts(UUID workspaceId) {
    return repository.findDraftSummaries(workspaceId).stream().map(this::toDraftSummaryView).toList();
  }

  @Transactional(readOnly = true)
  public DraftDetailsView getDraft(UUID workspaceId, UUID draftId) {
    return loadDraftView(workspaceId, draftId);
  }

  @Transactional
  public DraftDetailsView saveDraft(UUID actorUserId, UUID workspaceId, SaveDraftCommand command) {
    Instant now = Instant.now(clock);
    DraftRecord existing =
        command.draftId() == null ? null : loadDraftRecord(workspaceId, command.draftId());
    UUID draftId = existing == null ? UUID.randomUUID() : existing.id();

    UUID approvalRouteId =
        command.approvalRouteId() != null
            ? command.approvalRouteId()
            : existing == null ? null : existing.approvalRouteId();

    ScheduledJobRecord existingJob = repository.findScheduledJobByDraftId(draftId).orElse(null);
    ApprovalRequestRecord approvalRequest = repository.findApprovalRequest(draftId).orElse(null);
    List<MediaAssetView> assets = materializeAssets(actorUserId, workspaceId, command.mediaAssets());
    List<VariantInput> variants = normalizeVariants(command.variants());
    Map<String, Object> metadata = command.metadata() == null ? Map.of() : command.metadata();
    Map<String, Object> scheduledSummary =
        buildScheduledSummary(existingJob, variants, approvalStateValue(approvalRequest), assets);
    String lifecycleStatus = determineLifecycleStatus(approvalRequest, existingJob);

    if (existing == null) {
      repository.insertDraft(
          draftId,
          workspaceId,
          actorUserId,
          command.title(),
          command.body(),
          lifecycleStatus,
          command.primaryTimezone(),
          approvalRouteId,
          command.campaignLabel(),
          scheduledSummary,
          metadata,
          now);
    } else {
      repository.updateDraft(
          draftId,
          command.title(),
          command.body(),
          lifecycleStatus,
          command.primaryTimezone(),
          approvalRouteId,
          command.campaignLabel(),
          scheduledSummary,
          metadata,
          now);
    }

    repository.replaceDraftAssetLinks(draftId, assets.stream().map(MediaAssetView::assetId).toList());
    upsertVariants(draftId, variants);
    return loadDraftView(workspaceId, draftId);
  }

  @Transactional
  public ApprovalRequestView requestApproval(UUID actorUserId, UUID workspaceId, UUID draftId, ApprovalSubmission command) {
    DraftRecord draft = loadDraftRecord(workspaceId, draftId);
    Instant now = Instant.now(clock);
    ApprovalRequestRecord existing = repository.findApprovalRequest(draftId).orElse(null);

    UUID approvalRouteId = command.approvalRouteId() != null ? command.approvalRouteId() : draft.approvalRouteId();
    if (approvalRouteId == null) {
      throw new IllegalArgumentException("Approval route is required before submitting for approval");
    }

    UUID requestId = existing == null ? UUID.randomUUID() : existing.id();
    if (existing == null) {
      repository.insertApprovalRequest(requestId, draftId, approvalRouteId, "pending", actorUserId, now, null);
    } else {
      repository.updateApprovalRequest(requestId, approvalRouteId, "pending", now, null);
    }

    ScheduledJobRecord existingJob = repository.findScheduledJobByDraftId(draftId).orElse(null);
    repository.updateDraft(
        draftId,
        draft.title(),
        draft.body(),
        determineLifecycleStatus(
            new ApprovalRequestRecord(requestId, draftId, approvalRouteId, "pending", actorUserId, now, null),
            existingJob),
        draft.primaryTimezone(),
        approvalRouteId,
        draft.campaignLabel(),
        buildScheduledSummary(existingJob, repository.findVariants(draftId), approvalStateValue("pending"), repository.findDraftAssets(draftId)),
        draft.metadata(),
        draft.lastSavedAt());

    return toApprovalRequestView(
        repository.findApprovalRequest(draftId).orElseThrow(),
        repository.findApprovalDecisions(requestId));
  }

  @Transactional
  public ApprovalRequestView recordApprovalDecision(
      UUID actorUserId, UUID workspaceId, UUID draftId, ApprovalDecisionCommand command) {
    DraftRecord draft = loadDraftRecord(workspaceId, draftId);
    ApprovalRequestRecord approvalRequest =
        repository.findApprovalRequest(draftId)
            .orElseThrow(() -> new IllegalArgumentException("Approval request was not found for this draft"));
    Instant now = Instant.now(clock);

    repository.insertApprovalDecision(
        UUID.randomUUID(),
        approvalRequest.id(),
        command.stepNumber(),
        command.decision(),
        actorUserId,
        command.commentText(),
        now);

    String approvalStatus = switch (command.decision()) {
      case "approved" -> "approved";
      case "rejected" -> "rejected";
      default -> "changes_requested";
    };
    repository.updateApprovalRequest(
        approvalRequest.id(),
        approvalRequest.approvalRouteId(),
        approvalStatus,
        approvalRequest.requestedAt(),
        now);

    ScheduledJobRecord existingJob = repository.findScheduledJobByDraftId(draftId).orElse(null);
    repository.updateDraft(
        draftId,
        draft.title(),
        draft.body(),
        determineLifecycleStatus(
            new ApprovalRequestRecord(
                approvalRequest.id(),
                draftId,
                approvalRequest.approvalRouteId(),
                approvalStatus,
                approvalRequest.requestedByUserId(),
                approvalRequest.requestedAt(),
                now),
            existingJob),
        draft.primaryTimezone(),
        draft.approvalRouteId(),
        draft.campaignLabel(),
        buildScheduledSummary(existingJob, repository.findVariants(draftId), approvalStateValue(approvalStatus), repository.findDraftAssets(draftId)),
        draft.metadata(),
        draft.lastSavedAt());

    if ("approved".equals(approvalStatus)) {
      publishEvent(
          "PostApproved",
          Map.of(
              "workspaceId", workspaceId.toString(),
              "postId", draftId.toString(),
              "approverUserId", actorUserId.toString(),
              "approvedAt", now.toString()));
    }

    return toApprovalRequestView(
        repository.findApprovalRequest(draftId).orElseThrow(),
        repository.findApprovalDecisions(approvalRequest.id()));
  }

  @Transactional
  public ScheduledJobView scheduleDraft(UUID actorUserId, UUID workspaceId, UUID draftId, ScheduleCommand command) {
    DraftRecord draft = loadDraftRecord(workspaceId, draftId);
    List<VariantRecord> variants = repository.findVariants(draftId);
    if (variants.isEmpty()) {
      throw new IllegalArgumentException("At least one platform variant is required before scheduling");
    }
    if (variants.stream().allMatch(variant -> variant.targetAccountIds().isEmpty())) {
      throw new IllegalArgumentException("At least one connected account target is required before scheduling");
    }

    ScheduledJobRecord existing = repository.findScheduledJobByDraftId(draftId).orElse(null);
    if (existing == null) {
      repository.insertScheduledJob(
          UUID.randomUUID(),
          draftId,
          command.scheduledFor(),
          command.timezone(),
          "queued",
          0,
          command.scheduledFor());
    } else {
      repository.updateScheduledJob(
          existing.id(),
          command.scheduledFor(),
          command.timezone(),
          "queued",
          0,
          command.scheduledFor(),
          null,
          null);
    }

    ApprovalRequestRecord approvalRequest = repository.findApprovalRequest(draftId).orElse(null);
    ScheduledJobRecord job = repository.findScheduledJobByDraftId(draftId).orElseThrow();
    repository.updateDraft(
        draftId,
        draft.title(),
        draft.body(),
        determineLifecycleStatus(approvalRequest, job),
        draft.primaryTimezone(),
        draft.approvalRouteId(),
        draft.campaignLabel(),
        buildScheduledSummary(job, variants, approvalStateValue(approvalRequest), repository.findDraftAssets(draftId)),
        draft.metadata(),
        draft.lastSavedAt());

    publishEvent(
        "PostScheduled",
        Map.of(
            "workspaceId", workspaceId.toString(),
            "postId", draftId.toString(),
            "scheduledFor", command.scheduledFor().toString(),
            "channels", variants.stream().map(VariantRecord::provider).toList(),
            "approvalState", approvalStateValue(approvalRequest)));

    return toScheduledJobView(job, variants);
  }

  @Transactional
  public void cancelSchedule(UUID workspaceId, UUID draftId) {
    DraftRecord draft = loadDraftRecord(workspaceId, draftId);
    ScheduledJobRecord job =
        repository.findScheduledJobByDraftId(draftId)
            .orElseThrow(() -> new IllegalArgumentException("Scheduled job was not found for this draft"));
    repository.cancelScheduledJob(job.id());
    ApprovalRequestRecord approvalRequest = repository.findApprovalRequest(draftId).orElse(null);
    repository.updateDraft(
        draftId,
        draft.title(),
        draft.body(),
        determineLifecycleStatus(approvalRequest, repository.findScheduledJobByDraftId(draftId).orElseThrow()),
        draft.primaryTimezone(),
        draft.approvalRouteId(),
        draft.campaignLabel(),
        Map.of(),
        draft.metadata(),
        draft.lastSavedAt());
  }

  @Transactional(readOnly = true)
  public List<CalendarEntryView> listCalendarEntries(UUID workspaceId, Instant from, Instant to) {
    return repository.findCalendarEntries(workspaceId, from, to).stream()
        .map(this::toCalendarEntryView)
        .toList();
  }

  @Transactional
  public DispatchResult dispatchScheduledJob(UUID workspaceId, UUID jobId, UUID actorUserId) {
    repository.findScheduledJob(workspaceId, jobId)
        .orElseThrow(() -> new IllegalArgumentException("Scheduled job was not found"));
    return dispatchJob(jobId, actorUserId);
  }

  @Transactional
  public int dispatchDueJobs() {
    if (!properties.getDispatch().isEnabled()) {
      return 0;
    }
    Instant now = Instant.now(clock);
    int processed = 0;
    for (UUID jobId : repository.findDueScheduledJobIds(now, properties.getDispatch().getDueBatchSize())) {
      DispatchResult result = dispatchJob(jobId, null);
      if (!"skipped".equals(result.status())) {
        processed++;
      }
    }
    return processed;
  }

  private DispatchResult dispatchJob(UUID jobId, UUID actorUserIdOverride) {
    Instant now = Instant.now(clock);
    if (!repository.lockScheduledJob(jobId, now, properties.getDispatch().getWorkerId())) {
      return new DispatchResult(jobId, null, "skipped", Map.of(), List.of());
    }

    DispatchBundle bundle = repository.loadDispatchBundle(jobId);
    if (bundle.approvalRequest() != null && !"approved".equals(bundle.approvalRequest().status())) {
      repository.markScheduledJobQueued(jobId, bundle.scheduledJob().scheduledFor(), "APPROVAL_PENDING", "Draft is waiting for approval");
      return new DispatchResult(jobId, bundle.draft().id(), "awaiting_approval", Map.of(), List.of());
    }

    if (bundle.variants().isEmpty()) {
      failDispatch(bundle, "NO_VARIANTS", "No publish variants were configured");
      return new DispatchResult(jobId, bundle.draft().id(), "failed", Map.of(), List.of("No variants configured"));
    }

    repository.markScheduledJobDispatching(jobId, now);
    publishEvent(
        "PostPublishRequested",
        Map.of(
            "workspaceId", bundle.draft().workspaceId().toString(),
            "postId", bundle.draft().id().toString(),
            "publishWindowStart", bundle.scheduledJob().scheduledFor().toString()));

    Map<String, String> providerPostIds = new LinkedHashMap<>();
    List<String> failures = new ArrayList<>();
    List<String> mediaUrls = bundle.assets().stream().map(this::toMediaUri).toList();
    UUID actorUserId = actorUserIdOverride == null ? bundle.draft().authorUserId() : actorUserIdOverride;

    for (VariantRecord variant : bundle.variants()) {
      for (UUID connectedAccountId : variant.targetAccountIds()) {
        if (repository.hasSuccessfulAttempt(jobId, connectedAccountId)) {
          continue;
        }

        int attemptNumber = repository.nextAttemptNumber(jobId, connectedAccountId);
        UUID attemptId = UUID.randomUUID();
        repository.insertPublishAttempt(
            attemptId,
            jobId,
            variant.provider(),
            connectedAccountId,
            attemptNumber,
            "pending",
            Map.of(),
            Instant.now(clock));

        try {
          SocialPublishingGateway.PublishResult result =
              socialPublishingGateway.publish(
                  new SocialPublishingGateway.PublishCommand(
                      bundle.draft().workspaceId(),
                      actorUserId,
                      connectedAccountId,
                      variant.caption(),
                      variant.linkUrl(),
                      mediaUrls,
                      variant.firstComment(),
                      Map.of(
                          "draftId", bundle.draft().id(),
                          "scheduledJobId", jobId,
                          "variantId", variant.id(),
                          "providerOptions", variant.providerOptions(),
                          "campaignLabel", Objects.requireNonNullElse(bundle.draft().campaignLabel(), ""),
                          "futureAiHints", bundle.draft().metadata().getOrDefault("futureAiHints", List.of()))));
          repository.markPublishAttemptSuccess(
              attemptId,
              result.externalPostId(),
              result.providerPermalink(),
              result.publishedAt(),
              Map.of("provider", result.provider()));
          providerPostIds.put(variant.provider() + ":" + connectedAccountId, result.externalPostId());
        } catch (RuntimeException exception) {
          String reason = exception.getMessage() == null ? "Publishing request failed" : exception.getMessage();
          repository.markPublishAttemptFailure(
              attemptId,
              Instant.now(clock),
              Map.of("message", reason, "provider", variant.provider(), "connectedAccountId", connectedAccountId.toString()));
          failures.add(variant.provider() + ":" + connectedAccountId + ":" + reason);
        }
      }
    }

    if (!failures.isEmpty()) {
      failDispatch(bundle, "PUBLISH_FAILURE", String.join(" | ", failures));
      publishEvent(
          "PostPublishFailed",
          Map.of(
              "workspaceId", bundle.draft().workspaceId().toString(),
              "postId", bundle.draft().id().toString(),
              "provider", failures.getFirst().split(":")[0],
              "reason", failures.getFirst(),
              "failedAt", Instant.now(clock).toString()));
      return new DispatchResult(jobId, bundle.draft().id(), "retrying", providerPostIds, failures);
    }

    repository.markScheduledJobPublished(jobId, Instant.now(clock));
    DraftRecord draft = bundle.draft();
    repository.updateDraft(
        draft.id(),
        draft.title(),
        draft.body(),
        "published",
        draft.primaryTimezone(),
        draft.approvalRouteId(),
        draft.campaignLabel(),
        buildScheduledSummary(repository.findScheduledJobByDraftId(draft.id()).orElseThrow(), bundle.variants(), approvalStateValue("approved"), bundle.assets()),
        draft.metadata(),
        draft.lastSavedAt());

    publishEvent(
        "PostPublished",
        Map.of(
            "workspaceId", draft.workspaceId().toString(),
            "postId", draft.id().toString(),
            "providerPostIds", providerPostIds,
            "publishedAt", Instant.now(clock).toString()));

    return new DispatchResult(jobId, draft.id(), "published", providerPostIds, List.of());
  }

  private void failDispatch(DispatchBundle bundle, String errorCode, String errorMessage) {
    int nextRetryCount = bundle.scheduledJob().retryCount() + 1;
    Instant nextAttemptAt =
        nextRetryCount > properties.getDispatch().getMaxRetries()
            ? null
            : Instant.now(clock).plusMillis(computeRetryDelayMs(nextRetryCount));
    repository.markScheduledJobFailed(bundle.scheduledJob().id(), nextRetryCount, nextAttemptAt, errorCode, errorMessage);
    repository.updateDraft(
        bundle.draft().id(),
        bundle.draft().title(),
        bundle.draft().body(),
        "failed",
        bundle.draft().primaryTimezone(),
        bundle.draft().approvalRouteId(),
        bundle.draft().campaignLabel(),
        buildScheduledSummary(repository.findScheduledJobByDraftId(bundle.draft().id()).orElseThrow(), bundle.variants(), approvalStateValue(bundle.approvalRequest()), bundle.assets()),
        bundle.draft().metadata(),
        bundle.draft().lastSavedAt());
  }

  private long computeRetryDelayMs(int retryCount) {
    double rawDelay =
        properties.getDispatch().getInitialRetryDelayMs()
            * Math.pow(properties.getDispatch().getRetryMultiplier(), Math.max(0, retryCount - 1));
    return Math.min((long) rawDelay, properties.getDispatch().getMaxRetryDelayMs());
  }

  private DraftDetailsView loadDraftView(UUID workspaceId, UUID draftId) {
    DraftRecord draft = loadDraftRecord(workspaceId, draftId);
    List<MediaAssetView> assets = repository.findDraftAssets(draftId).stream().map(this::toMediaAssetView).toList();
    List<VariantView> variants = repository.findVariants(draftId).stream().map(this::toVariantView).toList();
    ApprovalRequestRecord approvalRequest = repository.findApprovalRequest(draftId).orElse(null);
    ScheduledJobRecord scheduledJob = repository.findScheduledJobByDraftId(draftId).orElse(null);
    return new DraftDetailsView(
        draft.id(),
        draft.workspaceId(),
        draft.authorUserId(),
        draft.title(),
        draft.body(),
        draft.lifecycleStatus(),
        draft.primaryTimezone(),
        draft.approvalRouteId(),
        draft.campaignLabel(),
        draft.scheduledSummary(),
        draft.metadata(),
        assets,
        variants,
        approvalRequest == null
            ? null
            : toApprovalRequestView(approvalRequest, repository.findApprovalDecisions(approvalRequest.id())),
        scheduledJob == null ? null : toScheduledJobView(scheduledJob, repository.findVariants(draftId)),
        draft.createdAt(),
        draft.updatedAt(),
        draft.lastSavedAt());
  }

  private DraftRecord loadDraftRecord(UUID workspaceId, UUID draftId) {
    return repository.findDraft(workspaceId, draftId)
        .orElseThrow(() -> new IllegalArgumentException("Draft was not found for the requested workspace"));
  }

  private void upsertVariants(UUID draftId, List<VariantInput> variants) {
    repository.deleteVariantsMissingProviders(draftId, variants.stream().map(VariantInput::provider).toList());
    for (VariantInput variant : variants) {
      UUID variantId = repository.findVariantId(draftId, variant.provider()).orElse(UUID.randomUUID());
      repository.upsertVariant(
          variantId,
          draftId,
          variant.provider(),
          variant.caption(),
          variant.linkUrl(),
          variant.firstComment(),
          variant.providerOptions());
      UUID persistedVariantId = repository.findVariantId(draftId, variant.provider()).orElseThrow();
      repository.replaceVariantTargets(persistedVariantId, variant.targetAccountIds());
    }
  }

  private List<MediaAssetView> materializeAssets(
      UUID actorUserId, UUID workspaceId, List<MediaAssetInput> mediaAssets) {
    if (mediaAssets == null) {
      return List.of();
    }
    List<MediaAssetView> views = new ArrayList<>();
    for (MediaAssetInput asset : mediaAssets) {
      if (asset.assetId() != null) {
        MediaAssetRecord existing =
            repository.findMediaAsset(workspaceId, asset.assetId())
                .orElseThrow(() -> new IllegalArgumentException("Attached media asset was not found"));
        views.add(toMediaAssetView(existing));
        continue;
      }

      UUID assetId = UUID.randomUUID();
      repository.insertMediaAsset(
          assetId,
          workspaceId,
          Objects.requireNonNullElse(asset.storageProvider(), "minio"),
          asset.bucketName(),
          asset.objectKey(),
          asset.mimeType(),
          asset.mediaKind(),
          asset.sizeBytes(),
          asset.sha256Checksum(),
          actorUserId,
          asset.altText(),
          asset.sourceUrl());
      views.add(toMediaAssetView(repository.findMediaAsset(workspaceId, assetId).orElseThrow()));
    }
    return views;
  }

  private List<VariantInput> normalizeVariants(List<VariantInput> variants) {
    if (variants == null) {
      return List.of();
    }
    Map<String, VariantInput> byProvider = new LinkedHashMap<>();
    for (VariantInput variant : variants) {
      if (!StringUtils.hasText(variant.provider())) {
        continue;
      }
      byProvider.put(
          variant.provider(),
          new VariantInput(
              variant.provider(),
              Objects.requireNonNullElse(variant.caption(), ""),
              variant.linkUrl(),
              variant.firstComment(),
              variant.providerOptions() == null ? Map.of() : variant.providerOptions(),
              variant.targetAccountIds() == null ? List.of() : List.copyOf(new LinkedHashSet<>(variant.targetAccountIds()))));
    }
    return List.copyOf(byProvider.values());
  }

  private Map<String, Object> buildScheduledSummary(
      ScheduledJobRecord job,
      List<?> variants,
      String approvalState,
      List<?> assets) {
    if (job == null || "cancelled".equals(job.status())) {
      return Map.of();
    }
    return Map.of(
        "scheduledFor", job.scheduledFor() == null ? null : job.scheduledFor().toString(),
        "timezone", job.timezone(),
        "jobStatus", job.status(),
        "approvalState", approvalState,
        "channels", variants.stream().map(this::variantProvider).toList(),
        "targetAccountIds",
        variants.stream().flatMap(variant -> variantTargetAccountIds(variant).stream()).distinct().map(UUID::toString).toList(),
        "assetCount", assets.size());
  }

  private String determineLifecycleStatus(ApprovalRequestRecord approvalRequest, ScheduledJobRecord job) {
    if (job != null) {
      return switch (job.status()) {
        case "published" -> "published";
        case "dispatching", "locked" -> "publishing";
        case "queued", "failed" -> approvalRequest != null && !"approved".equals(approvalRequest.status())
            ? "pending_approval"
            : "scheduled";
        case "cancelled" -> approvalRequest != null && "approved".equals(approvalRequest.status()) ? "approved" : "draft";
        default -> "draft";
      };
    }
    if (approvalRequest == null) {
      return "draft";
    }
    return switch (approvalRequest.status()) {
      case "pending" -> "pending_approval";
      case "approved" -> "approved";
      default -> "draft";
    };
  }

  private String approvalStateValue(ApprovalRequestRecord approvalRequest) {
    return approvalRequest == null ? "draft" : approvalStateValue(approvalRequest.status());
  }

  private String approvalStateValue(String rawStatus) {
    return switch (rawStatus) {
      case "pending" -> "pending_approval";
      case "approved" -> "approved";
      default -> "draft";
    };
  }

  private void publishEvent(String type, Map<String, Object> payload) {
    if (eventPublisher == null) {
      return;
    }
    String workspaceId = Objects.toString(payload.get("workspaceId"), null);
    eventPublisher.publish(
        new MessagePublishRequest(type, null, null, payload),
        workspaceId == null ? "post-scheduler-service" : workspaceId);
  }

  private String toMediaUri(MediaAssetRecord asset) {
    if (StringUtils.hasText(asset.sourceUrl())) {
      return asset.sourceUrl();
    }
    return "nexora-asset://%s/%s/%s".formatted(asset.storageProvider(), asset.bucketName(), asset.objectKey());
  }

  private DraftSummaryView toDraftSummaryView(DraftSummaryRecord record) {
    return new DraftSummaryView(
        record.id(),
        record.workspaceId(),
        record.authorUserId(),
        record.title(),
        record.body(),
        record.lifecycleStatus(),
        record.primaryTimezone(),
        record.approvalRouteId(),
        record.campaignLabel(),
        record.scheduledSummary(),
        record.metadata(),
        record.scheduledJobId(),
        record.scheduledFor(),
        record.scheduleStatus(),
        record.createdAt(),
        record.updatedAt(),
        record.lastSavedAt());
  }

  private MediaAssetView toMediaAssetView(MediaAssetRecord record) {
    return new MediaAssetView(
        record.id(),
        record.workspaceId(),
        record.storageProvider(),
        record.bucketName(),
        record.objectKey(),
        record.mimeType(),
        record.mediaKind(),
        record.sizeBytes(),
        record.sha256Checksum(),
        record.uploadedByUserId(),
        record.altText(),
        record.sourceUrl(),
        record.createdAt());
  }

  private VariantView toVariantView(VariantRecord record) {
    return new VariantView(
        record.id(),
        record.provider(),
        record.caption(),
        record.linkUrl(),
        record.firstComment(),
        record.providerOptions(),
        record.targetAccountIds(),
        record.createdAt());
  }

  private ApprovalRequestView toApprovalRequestView(
      ApprovalRequestRecord request, List<ApprovalDecisionRecord> decisions) {
    return new ApprovalRequestView(
        request.id(),
        request.approvalRouteId(),
        request.status(),
        request.requestedByUserId(),
        request.requestedAt(),
        request.resolvedAt(),
        decisions.stream()
            .map(
                decision ->
                    new ApprovalDecisionView(
                        decision.id(),
                        decision.stepNumber(),
                        decision.decision(),
                        decision.actedByUserId(),
                        decision.commentText(),
                        decision.actedAt()))
            .toList());
  }

  private ScheduledJobView toScheduledJobView(ScheduledJobRecord record, List<VariantRecord> variants) {
    return new ScheduledJobView(
        record.id(),
        record.draftId(),
        record.scheduledFor(),
        record.timezone(),
        record.status(),
        record.retryCount(),
        record.nextAttemptAt(),
        record.lastErrorCode(),
        record.lastErrorMessage(),
        variants.stream().map(VariantRecord::provider).toList(),
        variants.stream().flatMap(variant -> variant.targetAccountIds().stream()).distinct().toList(),
        record.createdAt(),
        record.lockedAt(),
        record.lockedBy(),
        record.dispatchedAt(),
        record.publishedAt(),
        record.cancelledAt());
  }

  private CalendarEntryView toCalendarEntryView(CalendarEntryRecord record) {
    Object channels = record.scheduledSummary().get("channels");
    return new CalendarEntryView(
        record.scheduledJobId(),
        record.draftId(),
        record.title(),
        record.campaignLabel(),
        record.lifecycleStatus(),
        record.approvalStatus(),
        record.jobStatus(),
        record.scheduledFor(),
        record.timezone(),
        channels instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of());
  }

  private String variantProvider(Object variant) {
    if (variant instanceof VariantInput value) {
      return value.provider();
    }
    if (variant instanceof VariantView value) {
      return value.provider();
    }
    if (variant instanceof VariantRecord value) {
      return value.provider();
    }
    return "unknown";
  }

  private List<UUID> variantTargetAccountIds(Object variant) {
    if (variant instanceof VariantInput value) {
      return value.targetAccountIds();
    }
    if (variant instanceof VariantView value) {
      return value.targetAccountIds();
    }
    if (variant instanceof VariantRecord value) {
      return value.targetAccountIds();
    }
    return List.of();
  }

  public record SaveDraftCommand(
      UUID draftId,
      String title,
      String body,
      String primaryTimezone,
      UUID approvalRouteId,
      String campaignLabel,
      Map<String, Object> metadata,
      List<MediaAssetInput> mediaAssets,
      List<VariantInput> variants) {}

  public record MediaAssetInput(
      UUID assetId,
      String storageProvider,
      String bucketName,
      String objectKey,
      String mimeType,
      String mediaKind,
      long sizeBytes,
      String sha256Checksum,
      String altText,
      String sourceUrl) {}

  public record VariantInput(
      String provider,
      String caption,
      String linkUrl,
      String firstComment,
      Map<String, Object> providerOptions,
      List<UUID> targetAccountIds) {}

  public record ApprovalSubmission(UUID approvalRouteId, String note) {}

  public record ApprovalDecisionCommand(int stepNumber, String decision, String commentText) {}

  public record ScheduleCommand(Instant scheduledFor, String timezone) {}

  public record DraftSummaryView(
      UUID draftId,
      UUID workspaceId,
      UUID authorUserId,
      String title,
      String body,
      String lifecycleStatus,
      String primaryTimezone,
      UUID approvalRouteId,
      String campaignLabel,
      Map<String, Object> scheduledSummary,
      Map<String, Object> metadata,
      UUID scheduledJobId,
      Instant scheduledFor,
      String scheduleStatus,
      Instant createdAt,
      Instant updatedAt,
      Instant lastSavedAt) {}

  public record DraftDetailsView(
      UUID draftId,
      UUID workspaceId,
      UUID authorUserId,
      String title,
      String body,
      String lifecycleStatus,
      String primaryTimezone,
      UUID approvalRouteId,
      String campaignLabel,
      Map<String, Object> scheduledSummary,
      Map<String, Object> metadata,
      List<MediaAssetView> assets,
      List<VariantView> variants,
      ApprovalRequestView approvalRequest,
      ScheduledJobView scheduledJob,
      Instant createdAt,
      Instant updatedAt,
      Instant lastSavedAt) {}

  public record MediaAssetView(
      UUID assetId,
      UUID workspaceId,
      String storageProvider,
      String bucketName,
      String objectKey,
      String mimeType,
      String mediaKind,
      long sizeBytes,
      String sha256Checksum,
      UUID uploadedByUserId,
      String altText,
      String sourceUrl,
      Instant createdAt) {}

  public record VariantView(
      UUID variantId,
      String provider,
      String caption,
      String linkUrl,
      String firstComment,
      Map<String, Object> providerOptions,
      List<UUID> targetAccountIds,
      Instant createdAt) {}

  public record ApprovalRequestView(
      UUID approvalRequestId,
      UUID approvalRouteId,
      String status,
      UUID requestedByUserId,
      Instant requestedAt,
      Instant resolvedAt,
      List<ApprovalDecisionView> decisions) {}

  public record ApprovalDecisionView(
      UUID approvalDecisionId,
      int stepNumber,
      String decision,
      UUID actedByUserId,
      String commentText,
      Instant actedAt) {}

  public record ScheduledJobView(
      UUID scheduledJobId,
      UUID draftId,
      Instant scheduledFor,
      String timezone,
      String status,
      int retryCount,
      Instant nextAttemptAt,
      String lastErrorCode,
      String lastErrorMessage,
      List<String> channels,
      List<UUID> targetAccountIds,
      Instant createdAt,
      Instant lockedAt,
      String lockedBy,
      Instant dispatchedAt,
      Instant publishedAt,
      Instant cancelledAt) {}

  public record CalendarEntryView(
      UUID scheduledJobId,
      UUID draftId,
      String title,
      String campaignLabel,
      String lifecycleStatus,
      String approvalStatus,
      String jobStatus,
      Instant scheduledFor,
      String timezone,
      List<String> channels) {}

  public record DispatchResult(
      UUID scheduledJobId,
      UUID draftId,
      String status,
      Map<String, String> providerPostIds,
      List<String> failures) {}
}
