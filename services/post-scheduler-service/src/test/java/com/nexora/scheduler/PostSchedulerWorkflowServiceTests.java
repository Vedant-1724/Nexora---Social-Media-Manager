package com.nexora.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.platform.core.messaging.NexoraEventPublisher;
import com.nexora.scheduler.config.PostSchedulerProperties;
import com.nexora.scheduler.repository.PostSchedulerRepository;
import com.nexora.scheduler.repository.PostSchedulerRepository.ApprovalDecisionRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.ApprovalRequestRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.DispatchBundle;
import com.nexora.scheduler.repository.PostSchedulerRepository.DraftRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.MediaAssetRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.ScheduledJobRecord;
import com.nexora.scheduler.repository.PostSchedulerRepository.VariantRecord;
import com.nexora.scheduler.service.PostSchedulerService;
import com.nexora.scheduler.service.SocialPublishingGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class PostSchedulerWorkflowServiceTests {

  @Mock private PostSchedulerRepository repository;
  @Mock private SocialPublishingGateway socialPublishingGateway;
  @Mock private ObjectProvider<NexoraEventPublisher> eventPublisherProvider;

  private PostSchedulerService postSchedulerService;
  private Clock clock;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC);
    postSchedulerService =
        new PostSchedulerService(
            repository,
            socialPublishingGateway,
            new PostSchedulerProperties(),
            clock,
            eventPublisherProvider);
  }

  @Test
  void scheduleDraftCreatesQueuedJob() {
    UUID workspaceId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID draftId = UUID.fromString("40000000-0000-0000-0000-000000000021");
    UUID actorUserId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    DraftRecord draft =
        new DraftRecord(
            draftId,
            workspaceId,
            actorUserId,
            "Launch",
            "Body",
            "draft",
            "Asia/Calcutta",
            null,
            "Campaign",
            Map.of(),
            Map.of(),
            Instant.now(clock),
            Instant.now(clock),
            Instant.now(clock));

    when(repository.findDraft(workspaceId, draftId)).thenReturn(Optional.of(draft));
    when(repository.findVariants(draftId))
        .thenReturn(
            List.of(
                new VariantRecord(
                    UUID.fromString("40000000-0000-0000-0000-000000000031"),
                    draftId,
                    "meta",
                    "Caption",
                    null,
                    null,
                    Map.of(),
                    List.of(UUID.fromString("30000000-0000-0000-0000-000000000011")),
                    Instant.now(clock))));
    when(repository.findScheduledJobByDraftId(draftId))
        .thenReturn(
            Optional.empty(),
            Optional.of(
                new ScheduledJobRecord(
                    UUID.fromString("40000000-0000-0000-0000-000000000051"),
                    draftId,
                    Instant.parse("2026-04-02T12:00:00Z"),
                    "Asia/Calcutta",
                    "queued",
                    0,
                    Instant.parse("2026-04-02T12:00:00Z"),
                    null,
                    null,
                    Instant.now(clock),
                    null,
                    null,
                    null,
                    null,
                    null)));
    when(repository.findApprovalRequest(draftId)).thenReturn(Optional.empty());
    when(repository.findDraftAssets(draftId)).thenReturn(List.of());

    PostSchedulerService.ScheduledJobView result =
        postSchedulerService.scheduleDraft(
            actorUserId,
            workspaceId,
            draftId,
            new PostSchedulerService.ScheduleCommand(
                Instant.parse("2026-04-02T12:00:00Z"), "Asia/Calcutta"));

    assertThat(result.status()).isEqualTo("queued");
    verify(repository).insertScheduledJob(any(), eq(draftId), any(), eq("Asia/Calcutta"), eq("queued"), eq(0), any());
  }

  @Test
  void dispatchScheduledJobPublishesVariantsAndMarksJobPublished() {
    UUID workspaceId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID draftId = UUID.fromString("40000000-0000-0000-0000-000000000021");
    UUID jobId = UUID.fromString("40000000-0000-0000-0000-000000000051");
    UUID actorUserId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    UUID connectedAccountId = UUID.fromString("30000000-0000-0000-0000-000000000011");

    when(repository.findScheduledJob(workspaceId, jobId))
        .thenReturn(
            Optional.of(
                new ScheduledJobRecord(
                    jobId,
                    draftId,
                    Instant.parse("2026-04-01T11:00:00Z"),
                    "Asia/Calcutta",
                    "queued",
                    0,
                    Instant.parse("2026-04-01T11:00:00Z"),
                    null,
                    null,
                    Instant.now(clock),
                    null,
                    null,
                    null,
                    null,
                    null)));
    when(repository.lockScheduledJob(eq(jobId), any(), any())).thenReturn(true);
    when(repository.loadDispatchBundle(jobId))
        .thenReturn(
            new DispatchBundle(
                new DraftRecord(
                    draftId,
                    workspaceId,
                    actorUserId,
                    "Launch",
                    "Body",
                    "scheduled",
                    "Asia/Calcutta",
                    null,
                    "Campaign",
                    Map.of(),
                    Map.of(),
                    Instant.now(clock),
                    Instant.now(clock),
                    Instant.now(clock)),
                List.of(
                    new MediaAssetRecord(
                        UUID.fromString("40000000-0000-0000-0000-000000000011"),
                        workspaceId,
                        "minio",
                        "nexora-assets",
                        "hero.png",
                        "image/png",
                        "image",
                        1024,
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        actorUserId,
                        "Hero",
                        "https://cdn.example/hero.png",
                        Instant.now(clock))),
                List.of(
                    new VariantRecord(
                        UUID.fromString("40000000-0000-0000-0000-000000000031"),
                        draftId,
                        "meta",
                        "Caption",
                        null,
                        null,
                        Map.of(),
                        List.of(connectedAccountId),
                        Instant.now(clock))),
                new ApprovalRequestRecord(
                    UUID.fromString("40000000-0000-0000-0000-000000000041"),
                    draftId,
                    null,
                    "approved",
                    actorUserId,
                    Instant.now(clock),
                    Instant.now(clock)),
                List.of(
                    new ApprovalDecisionRecord(
                        UUID.fromString("40000000-0000-0000-0000-000000000042"),
                        UUID.fromString("40000000-0000-0000-0000-000000000041"),
                        1,
                        "approved",
                        actorUserId,
                        "ok",
                        Instant.now(clock))),
                new ScheduledJobRecord(
                    jobId,
                    draftId,
                    Instant.parse("2026-04-01T11:00:00Z"),
                    "Asia/Calcutta",
                    "queued",
                    0,
                    Instant.parse("2026-04-01T11:00:00Z"),
                    null,
                    null,
                    Instant.now(clock),
                    null,
                    null,
                    null,
                    null,
                    null)));
    when(repository.hasSuccessfulAttempt(jobId, connectedAccountId)).thenReturn(false);
    when(repository.nextAttemptNumber(jobId, connectedAccountId)).thenReturn(1);
    when(socialPublishingGateway.publish(any()))
        .thenReturn(
            new SocialPublishingGateway.PublishResult(
                connectedAccountId,
                "meta",
                "meta-post-1",
                "https://meta.example/posts/1",
                Instant.now(clock)));
    when(repository.findScheduledJobByDraftId(draftId))
        .thenReturn(
            Optional.of(
                new ScheduledJobRecord(
                    jobId,
                    draftId,
                    Instant.parse("2026-04-01T11:00:00Z"),
                    "Asia/Calcutta",
                    "published",
                    0,
                    null,
                    null,
                    null,
                    Instant.now(clock),
                    null,
                    null,
                    Instant.now(clock),
                    Instant.now(clock),
                    null)));

    PostSchedulerService.DispatchResult result =
        postSchedulerService.dispatchScheduledJob(workspaceId, jobId, actorUserId);

    assertThat(result.status()).isEqualTo("published");
    assertThat(result.providerPostIds()).containsValue("meta-post-1");
    verify(repository).markScheduledJobPublished(eq(jobId), any());
  }
}
