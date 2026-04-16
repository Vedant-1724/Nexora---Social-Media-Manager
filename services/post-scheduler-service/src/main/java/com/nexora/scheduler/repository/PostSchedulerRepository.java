package com.nexora.scheduler.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostSchedulerRepository {

  private static final TypeReference<Map<String, Object>> STRING_MAP = new TypeReference<>() {};

  private final JdbcClient jdbcClient;
  private final ObjectMapper objectMapper;

  public PostSchedulerRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
    this.jdbcClient = jdbcClient;
    this.objectMapper = objectMapper;
  }

  public List<DraftSummaryRecord> findDraftSummaries(UUID workspaceId) {
    return jdbcClient.sql(
            """
            SELECT d.id, d.workspace_id, d.author_user_id, d.title, d.body, d.lifecycle_status, d.primary_timezone,
                   d.approval_route_id, d.campaign_label, d.scheduled_summary, d.metadata, d.created_at, d.updated_at, d.last_saved_at,
                   sj.id AS scheduled_job_id, sj.scheduled_for, sj.status AS schedule_status
            FROM post_drafts d
            LEFT JOIN scheduled_jobs sj ON sj.draft_id = d.id
            WHERE d.workspace_id = :workspaceId
            ORDER BY d.updated_at DESC
            """)
        .param("workspaceId", workspaceId)
        .query(this::mapDraftSummary)
        .list();
  }

  public Optional<DraftRecord> findDraft(UUID workspaceId, UUID draftId) {
    return jdbcClient.sql(
            """
            SELECT id, workspace_id, author_user_id, title, body, lifecycle_status, primary_timezone,
                   approval_route_id, campaign_label, scheduled_summary, metadata, created_at, updated_at, last_saved_at
            FROM post_drafts
            WHERE workspace_id = :workspaceId AND id = :draftId
            """)
        .param("workspaceId", workspaceId)
        .param("draftId", draftId)
        .query(this::mapDraft)
        .optional();
  }

  public void insertDraft(
      UUID id,
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
      Instant lastSavedAt) {
    jdbcClient.sql(
            """
            INSERT INTO post_drafts (
              id, workspace_id, author_user_id, title, body, lifecycle_status, primary_timezone,
              approval_route_id, campaign_label, scheduled_summary, metadata, last_saved_at
            ) VALUES (
              :id, :workspaceId, :authorUserId, :title, :body, :lifecycleStatus, :primaryTimezone,
              :approvalRouteId, :campaignLabel, CAST(:scheduledSummary AS jsonb), CAST(:metadata AS jsonb), :lastSavedAt
            )
            """)
        .param("id", id)
        .param("workspaceId", workspaceId)
        .param("authorUserId", authorUserId)
        .param("title", title)
        .param("body", body)
        .param("lifecycleStatus", lifecycleStatus)
        .param("primaryTimezone", primaryTimezone)
        .param("approvalRouteId", approvalRouteId)
        .param("campaignLabel", campaignLabel)
        .param("scheduledSummary", toJson(scheduledSummary))
        .param("metadata", toJson(metadata))
        .param("lastSavedAt", timestamp(lastSavedAt))
        .update();
  }

  public void updateDraft(
      UUID id,
      String title,
      String body,
      String lifecycleStatus,
      String primaryTimezone,
      UUID approvalRouteId,
      String campaignLabel,
      Map<String, Object> scheduledSummary,
      Map<String, Object> metadata,
      Instant lastSavedAt) {
    jdbcClient.sql(
            """
            UPDATE post_drafts
            SET title = :title,
                body = :body,
                lifecycle_status = :lifecycleStatus,
                primary_timezone = :primaryTimezone,
                approval_route_id = :approvalRouteId,
                campaign_label = :campaignLabel,
                scheduled_summary = CAST(:scheduledSummary AS jsonb),
                metadata = CAST(:metadata AS jsonb),
                last_saved_at = :lastSavedAt,
                updated_at = NOW()
            WHERE id = :id
            """)
        .param("id", id)
        .param("title", title)
        .param("body", body)
        .param("lifecycleStatus", lifecycleStatus)
        .param("primaryTimezone", primaryTimezone)
        .param("approvalRouteId", approvalRouteId)
        .param("campaignLabel", campaignLabel)
        .param("scheduledSummary", toJson(scheduledSummary))
        .param("metadata", toJson(metadata))
        .param("lastSavedAt", timestamp(lastSavedAt))
        .update();
  }

  public Optional<MediaAssetRecord> findMediaAsset(UUID workspaceId, UUID assetId) {
    return jdbcClient.sql(
            """
            SELECT id, workspace_id, storage_provider, bucket_name, object_key, mime_type, media_kind,
                   size_bytes, sha256_checksum, uploaded_by_user_id, alt_text, source_url, created_at
            FROM media_assets
            WHERE workspace_id = :workspaceId AND id = :assetId
            """)
        .param("workspaceId", workspaceId)
        .param("assetId", assetId)
        .query(this::mapMediaAsset)
        .optional();
  }

  public List<MediaAssetRecord> findDraftAssets(UUID draftId) {
    return jdbcClient.sql(
            """
            SELECT a.id, a.workspace_id, a.storage_provider, a.bucket_name, a.object_key, a.mime_type, a.media_kind,
                   a.size_bytes, a.sha256_checksum, a.uploaded_by_user_id, a.alt_text, a.source_url, a.created_at
            FROM draft_asset_links dal
            JOIN media_assets a ON a.id = dal.asset_id
            WHERE dal.draft_id = :draftId
            ORDER BY dal.sort_order ASC, a.created_at ASC
            """)
        .param("draftId", draftId)
        .query(this::mapMediaAsset)
        .list();
  }

  public void insertMediaAsset(
      UUID id,
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
      String sourceUrl) {
    jdbcClient.sql(
            """
            INSERT INTO media_assets (
              id, workspace_id, storage_provider, bucket_name, object_key, mime_type, media_kind,
              size_bytes, sha256_checksum, uploaded_by_user_id, alt_text, source_url
            ) VALUES (
              :id, :workspaceId, :storageProvider, :bucketName, :objectKey, :mimeType, :mediaKind,
              :sizeBytes, :sha256Checksum, :uploadedByUserId, :altText, :sourceUrl
            )
            """)
        .param("id", id)
        .param("workspaceId", workspaceId)
        .param("storageProvider", storageProvider)
        .param("bucketName", bucketName)
        .param("objectKey", objectKey)
        .param("mimeType", mimeType)
        .param("mediaKind", mediaKind)
        .param("sizeBytes", sizeBytes)
        .param("sha256Checksum", sha256Checksum)
        .param("uploadedByUserId", uploadedByUserId)
        .param("altText", altText)
        .param("sourceUrl", sourceUrl)
        .update();
  }

  public void replaceDraftAssetLinks(UUID draftId, List<UUID> assetIds) {
    jdbcClient.sql("DELETE FROM draft_asset_links WHERE draft_id = :draftId")
        .param("draftId", draftId)
        .update();
    for (int index = 0; index < assetIds.size(); index++) {
      jdbcClient.sql(
              """
              INSERT INTO draft_asset_links (draft_id, asset_id, sort_order)
              VALUES (:draftId, :assetId, :sortOrder)
              """)
          .param("draftId", draftId)
          .param("assetId", assetIds.get(index))
          .param("sortOrder", index)
          .update();
    }
  }

  public List<VariantRecord> findVariants(UUID draftId) {
    List<VariantRecord> variants =
        jdbcClient.sql(
                """
                SELECT id, draft_id, provider, caption, link_url, first_comment, provider_options, created_at
                FROM post_variants
                WHERE draft_id = :draftId
                ORDER BY provider ASC
                """)
            .param("draftId", draftId)
            .query(this::mapVariant)
            .list();
    Map<UUID, List<UUID>> targets = findVariantTargets(draftId);
    return variants.stream()
        .map(
            variant ->
                new VariantRecord(
                    variant.id(),
                    variant.draftId(),
                    variant.provider(),
                    variant.caption(),
                    variant.linkUrl(),
                    variant.firstComment(),
                    variant.providerOptions(),
                    targets.getOrDefault(variant.id(), List.of()),
                    variant.createdAt()))
        .toList();
  }

  public Optional<UUID> findVariantId(UUID draftId, String provider) {
    return jdbcClient.sql(
            """
            SELECT id
            FROM post_variants
            WHERE draft_id = :draftId AND provider = :provider
            """)
        .param("draftId", draftId)
        .param("provider", provider)
        .query((resultSet, rowNum) -> resultSet.getObject("id", UUID.class))
        .optional();
  }

  public void upsertVariant(
      UUID id,
      UUID draftId,
      String provider,
      String caption,
      String linkUrl,
      String firstComment,
      Map<String, Object> providerOptions) {
    jdbcClient.sql(
            """
            INSERT INTO post_variants (id, draft_id, provider, caption, link_url, first_comment, provider_options)
            VALUES (:id, :draftId, :provider, :caption, :linkUrl, :firstComment, CAST(:providerOptions AS jsonb))
            ON CONFLICT (draft_id, provider)
            DO UPDATE SET caption = EXCLUDED.caption,
                          link_url = EXCLUDED.link_url,
                          first_comment = EXCLUDED.first_comment,
                          provider_options = EXCLUDED.provider_options
            """)
        .param("id", id)
        .param("draftId", draftId)
        .param("provider", provider)
        .param("caption", caption)
        .param("linkUrl", linkUrl)
        .param("firstComment", firstComment)
        .param("providerOptions", toJson(providerOptions))
        .update();
  }

  public void deleteVariantsMissingProviders(UUID draftId, List<String> providers) {
    if (providers.isEmpty()) {
      jdbcClient.sql("DELETE FROM post_variants WHERE draft_id = :draftId")
          .param("draftId", draftId)
          .update();
      return;
    }
    StringBuilder sql = new StringBuilder("DELETE FROM post_variants WHERE draft_id = :draftId AND provider NOT IN (");
    for (int index = 0; index < providers.size(); index++) {
      if (index > 0) {
        sql.append(", ");
      }
      sql.append(":provider").append(index);
    }
    sql.append(")");
    JdbcClient.StatementSpec statement = jdbcClient.sql(sql.toString()).param("draftId", draftId);
    for (int index = 0; index < providers.size(); index++) {
      statement.param("provider" + index, providers.get(index));
    }
    statement.update();
  }

  public void replaceVariantTargets(UUID variantId, List<UUID> connectedAccountIds) {
    jdbcClient.sql("DELETE FROM variant_targets WHERE variant_id = :variantId")
        .param("variantId", variantId)
        .update();
    for (UUID connectedAccountId : connectedAccountIds) {
      jdbcClient.sql(
              """
              INSERT INTO variant_targets (variant_id, connected_account_id)
              VALUES (:variantId, :connectedAccountId)
              """)
          .param("variantId", variantId)
          .param("connectedAccountId", connectedAccountId)
          .update();
    }
  }

  public Optional<ApprovalRequestRecord> findApprovalRequest(UUID draftId) {
    return jdbcClient.sql(
            """
            SELECT id, draft_id, approval_route_id, status, requested_by_user_id, requested_at, resolved_at
            FROM approval_requests
            WHERE draft_id = :draftId
            """)
        .param("draftId", draftId)
        .query(this::mapApprovalRequest)
        .optional();
  }

  public List<ApprovalDecisionRecord> findApprovalDecisions(UUID approvalRequestId) {
    return jdbcClient.sql(
            """
            SELECT id, approval_request_id, step_number, decision, acted_by_user_id, comment_text, acted_at
            FROM approval_decisions
            WHERE approval_request_id = :approvalRequestId
            ORDER BY step_number ASC, acted_at ASC
            """)
        .param("approvalRequestId", approvalRequestId)
        .query(this::mapApprovalDecision)
        .list();
  }

  public void insertApprovalRequest(
      UUID id,
      UUID draftId,
      UUID approvalRouteId,
      String status,
      UUID requestedByUserId,
      Instant requestedAt,
      Instant resolvedAt) {
    jdbcClient.sql(
            """
            INSERT INTO approval_requests (
              id, draft_id, approval_route_id, status, requested_by_user_id, requested_at, resolved_at
            ) VALUES (
              :id, :draftId, :approvalRouteId, :status, :requestedByUserId, :requestedAt, :resolvedAt
            )
            """)
        .param("id", id)
        .param("draftId", draftId)
        .param("approvalRouteId", approvalRouteId)
        .param("status", status)
        .param("requestedByUserId", requestedByUserId)
        .param("requestedAt", timestamp(requestedAt))
        .param("resolvedAt", timestamp(resolvedAt))
        .update();
  }

  public void updateApprovalRequest(
      UUID id, UUID approvalRouteId, String status, Instant requestedAt, Instant resolvedAt) {
    jdbcClient.sql(
            """
            UPDATE approval_requests
            SET approval_route_id = :approvalRouteId,
                status = :status,
                requested_at = :requestedAt,
                resolved_at = :resolvedAt
            WHERE id = :id
            """)
        .param("id", id)
        .param("approvalRouteId", approvalRouteId)
        .param("status", status)
        .param("requestedAt", timestamp(requestedAt))
        .param("resolvedAt", timestamp(resolvedAt))
        .update();
  }

  public void insertApprovalDecision(
      UUID id,
      UUID approvalRequestId,
      int stepNumber,
      String decision,
      UUID actedByUserId,
      String commentText,
      Instant actedAt) {
    jdbcClient.sql(
            """
            INSERT INTO approval_decisions (
              id, approval_request_id, step_number, decision, acted_by_user_id, comment_text, acted_at
            ) VALUES (
              :id, :approvalRequestId, :stepNumber, :decision, :actedByUserId, :commentText, :actedAt
            )
            """)
        .param("id", id)
        .param("approvalRequestId", approvalRequestId)
        .param("stepNumber", stepNumber)
        .param("decision", decision)
        .param("actedByUserId", actedByUserId)
        .param("commentText", commentText)
        .param("actedAt", timestamp(actedAt))
        .update();
  }

  public Optional<ScheduledJobRecord> findScheduledJobByDraftId(UUID draftId) {
    return jdbcClient.sql(
            """
            SELECT id, draft_id, scheduled_for, timezone, status, retry_count, next_attempt_at,
                   last_error_code, last_error_message, created_at, locked_at, locked_by, dispatched_at, published_at, cancelled_at
            FROM scheduled_jobs
            WHERE draft_id = :draftId
            """)
        .param("draftId", draftId)
        .query(this::mapScheduledJob)
        .optional();
  }

  public Optional<ScheduledJobRecord> findScheduledJob(UUID workspaceId, UUID jobId) {
    return jdbcClient.sql(
            """
            SELECT sj.id, sj.draft_id, sj.scheduled_for, sj.timezone, sj.status, sj.retry_count, sj.next_attempt_at,
                   sj.last_error_code, sj.last_error_message, sj.created_at, sj.locked_at, sj.locked_by, sj.dispatched_at, sj.published_at, sj.cancelled_at
            FROM scheduled_jobs sj
            JOIN post_drafts d ON d.id = sj.draft_id
            WHERE sj.id = :jobId AND d.workspace_id = :workspaceId
            """)
        .param("jobId", jobId)
        .param("workspaceId", workspaceId)
        .query(this::mapScheduledJob)
        .optional();
  }

  public void insertScheduledJob(
      UUID id, UUID draftId, Instant scheduledFor, String timezone, String status, int retryCount, Instant nextAttemptAt) {
    jdbcClient.sql(
            """
            INSERT INTO scheduled_jobs (
              id, draft_id, scheduled_for, timezone, status, retry_count, next_attempt_at
            ) VALUES (
              :id, :draftId, :scheduledFor, :timezone, :status, :retryCount, :nextAttemptAt
            )
            """)
        .param("id", id)
        .param("draftId", draftId)
        .param("scheduledFor", timestamp(scheduledFor))
        .param("timezone", timezone)
        .param("status", status)
        .param("retryCount", retryCount)
        .param("nextAttemptAt", timestamp(nextAttemptAt))
        .update();
  }

  public void updateScheduledJob(
      UUID id,
      Instant scheduledFor,
      String timezone,
      String status,
      int retryCount,
      Instant nextAttemptAt,
      String lastErrorCode,
      String lastErrorMessage) {
    jdbcClient.sql(
            """
            UPDATE scheduled_jobs
            SET scheduled_for = :scheduledFor,
                timezone = :timezone,
                status = :status,
                retry_count = :retryCount,
                next_attempt_at = :nextAttemptAt,
                last_error_code = :lastErrorCode,
                last_error_message = :lastErrorMessage,
                locked_at = NULL,
                locked_by = NULL,
                dispatched_at = NULL,
                published_at = NULL,
                cancelled_at = CASE WHEN :status = 'cancelled' THEN NOW() ELSE cancelled_at END
            WHERE id = :id
            """)
        .param("id", id)
        .param("scheduledFor", timestamp(scheduledFor))
        .param("timezone", timezone)
        .param("status", status)
        .param("retryCount", retryCount)
        .param("nextAttemptAt", timestamp(nextAttemptAt))
        .param("lastErrorCode", lastErrorCode)
        .param("lastErrorMessage", lastErrorMessage)
        .update();
  }

  public boolean lockScheduledJob(UUID jobId, Instant now, String workerId) {
    int updated =
        jdbcClient.sql(
                """
                UPDATE scheduled_jobs
                SET status = 'locked',
                    locked_at = :now,
                    locked_by = :workerId,
                    last_error_code = NULL,
                    last_error_message = NULL
                WHERE id = :jobId
                  AND status IN ('queued', 'failed')
                  AND next_attempt_at IS NOT NULL
                  AND next_attempt_at <= :now
                """)
            .param("jobId", jobId)
            .param("now", timestamp(now))
            .param("workerId", workerId)
            .update();
    return updated > 0;
  }

  public void markScheduledJobQueued(UUID jobId, Instant nextAttemptAt, String lastErrorCode, String lastErrorMessage) {
    jdbcClient.sql(
            """
            UPDATE scheduled_jobs
            SET status = 'queued',
                next_attempt_at = :nextAttemptAt,
                last_error_code = :lastErrorCode,
                last_error_message = :lastErrorMessage,
                locked_at = NULL,
                locked_by = NULL
            WHERE id = :jobId
            """)
        .param("jobId", jobId)
        .param("nextAttemptAt", timestamp(nextAttemptAt))
        .param("lastErrorCode", lastErrorCode)
        .param("lastErrorMessage", lastErrorMessage)
        .update();
  }

  public void markScheduledJobDispatching(UUID jobId, Instant dispatchedAt) {
    jdbcClient.sql(
            """
            UPDATE scheduled_jobs
            SET status = 'dispatching',
                dispatched_at = :dispatchedAt
            WHERE id = :jobId
            """)
        .param("jobId", jobId)
        .param("dispatchedAt", timestamp(dispatchedAt))
        .update();
  }

  public void markScheduledJobPublished(UUID jobId, Instant publishedAt) {
    jdbcClient.sql(
            """
            UPDATE scheduled_jobs
            SET status = 'published',
                published_at = :publishedAt,
                next_attempt_at = NULL,
                locked_at = NULL,
                locked_by = NULL
            WHERE id = :jobId
            """)
        .param("jobId", jobId)
        .param("publishedAt", timestamp(publishedAt))
        .update();
  }

  public void markScheduledJobFailed(
      UUID jobId, int retryCount, Instant nextAttemptAt, String lastErrorCode, String lastErrorMessage) {
    jdbcClient.sql(
            """
            UPDATE scheduled_jobs
            SET status = 'failed',
                retry_count = :retryCount,
                next_attempt_at = :nextAttemptAt,
                last_error_code = :lastErrorCode,
                last_error_message = :lastErrorMessage,
                locked_at = NULL,
                locked_by = NULL
            WHERE id = :jobId
            """)
        .param("jobId", jobId)
        .param("retryCount", retryCount)
        .param("nextAttemptAt", timestamp(nextAttemptAt))
        .param("lastErrorCode", lastErrorCode)
        .param("lastErrorMessage", lastErrorMessage)
        .update();
  }

  public void cancelScheduledJob(UUID jobId) {
    jdbcClient.sql(
            """
            UPDATE scheduled_jobs
            SET status = 'cancelled',
                next_attempt_at = NULL,
                cancelled_at = NOW(),
                locked_at = NULL,
                locked_by = NULL
            WHERE id = :jobId
            """)
        .param("jobId", jobId)
        .update();
  }

  public List<UUID> findDueScheduledJobIds(Instant now, int limit) {
    return jdbcClient.sql(
            """
            SELECT sj.id
            FROM scheduled_jobs sj
            LEFT JOIN approval_requests ar ON ar.draft_id = sj.draft_id
            WHERE sj.status IN ('queued', 'failed')
              AND sj.next_attempt_at IS NOT NULL
              AND sj.next_attempt_at <= :now
              AND (ar.id IS NULL OR ar.status = 'approved')
            ORDER BY sj.next_attempt_at ASC
            LIMIT :limit
            """)
        .param("now", timestamp(now))
        .param("limit", limit)
        .query((resultSet, rowNum) -> resultSet.getObject("id", UUID.class))
        .list();
  }

  public int nextAttemptNumber(UUID scheduledJobId, UUID connectedAccountId) {
    Integer current =
        jdbcClient.sql(
                """
                SELECT COALESCE(MAX(attempt_number), 0)
                FROM publish_attempts
                WHERE scheduled_job_id = :scheduledJobId AND connected_account_id = :connectedAccountId
                """)
            .param("scheduledJobId", scheduledJobId)
            .param("connectedAccountId", connectedAccountId)
            .query((resultSet, rowNum) -> resultSet.getInt(1))
            .single();
    return current + 1;
  }

  public boolean hasSuccessfulAttempt(UUID scheduledJobId, UUID connectedAccountId) {
    Integer count =
        jdbcClient.sql(
                """
                SELECT COUNT(*)
                FROM publish_attempts
                WHERE scheduled_job_id = :scheduledJobId
                  AND connected_account_id = :connectedAccountId
                  AND outcome = 'success'
                """)
            .param("scheduledJobId", scheduledJobId)
            .param("connectedAccountId", connectedAccountId)
            .query((resultSet, rowNum) -> resultSet.getInt(1))
            .single();
    return count != null && count > 0;
  }

  public void insertPublishAttempt(
      UUID id,
      UUID scheduledJobId,
      String provider,
      UUID connectedAccountId,
      int attemptNumber,
      String outcome,
      Map<String, Object> errorDetails,
      Instant startedAt) {
    jdbcClient.sql(
            """
            INSERT INTO publish_attempts (
              id, scheduled_job_id, provider, connected_account_id, attempt_number, outcome, error_details, started_at
            ) VALUES (
              :id, :scheduledJobId, :provider, :connectedAccountId, :attemptNumber, :outcome, CAST(:errorDetails AS jsonb), :startedAt
            )
            """)
        .param("id", id)
        .param("scheduledJobId", scheduledJobId)
        .param("provider", provider)
        .param("connectedAccountId", connectedAccountId)
        .param("attemptNumber", attemptNumber)
        .param("outcome", outcome)
        .param("errorDetails", toJson(errorDetails))
        .param("startedAt", timestamp(startedAt))
        .update();
  }

  public void markPublishAttemptSuccess(
      UUID attemptId,
      String providerPostId,
      String providerPermalink,
      Instant completedAt,
      Map<String, Object> errorDetails) {
    jdbcClient.sql(
            """
            UPDATE publish_attempts
            SET outcome = 'success',
                provider_post_id = :providerPostId,
                provider_permalink = :providerPermalink,
                error_details = CAST(:errorDetails AS jsonb),
                completed_at = :completedAt
            WHERE id = :attemptId
            """)
        .param("attemptId", attemptId)
        .param("providerPostId", providerPostId)
        .param("providerPermalink", providerPermalink)
        .param("errorDetails", toJson(errorDetails))
        .param("completedAt", timestamp(completedAt))
        .update();
  }

  public void markPublishAttemptFailure(UUID attemptId, Instant completedAt, Map<String, Object> errorDetails) {
    jdbcClient.sql(
            """
            UPDATE publish_attempts
            SET outcome = 'failure',
                error_details = CAST(:errorDetails AS jsonb),
                completed_at = :completedAt
            WHERE id = :attemptId
            """)
        .param("attemptId", attemptId)
        .param("errorDetails", toJson(errorDetails))
        .param("completedAt", timestamp(completedAt))
        .update();
  }

  public List<CalendarEntryRecord> findCalendarEntries(UUID workspaceId, Instant from, Instant to) {
    return jdbcClient.sql(
            """
            SELECT sj.id AS scheduled_job_id, d.id AS draft_id, d.title, d.campaign_label, d.lifecycle_status,
                   COALESCE(ar.status, 'not_requested') AS approval_status, sj.status AS job_status, sj.scheduled_for,
                   sj.timezone, d.scheduled_summary
            FROM scheduled_jobs sj
            JOIN post_drafts d ON d.id = sj.draft_id
            LEFT JOIN approval_requests ar ON ar.draft_id = d.id
            WHERE d.workspace_id = :workspaceId
              AND sj.scheduled_for >= :fromValue
              AND sj.scheduled_for <= :toValue
            ORDER BY sj.scheduled_for ASC
            """)
        .param("workspaceId", workspaceId)
        .param("fromValue", timestamp(from))
        .param("toValue", timestamp(to))
        .query(this::mapCalendarEntry)
        .list();
  }

  public DispatchBundle loadDispatchBundle(UUID jobId) {
    ScheduledJobRecord job =
        jdbcClient.sql(
                """
                SELECT id, draft_id, scheduled_for, timezone, status, retry_count, next_attempt_at,
                       last_error_code, last_error_message, created_at, locked_at, locked_by, dispatched_at, published_at, cancelled_at
                FROM scheduled_jobs
                WHERE id = :jobId
                """)
            .param("jobId", jobId)
            .query(this::mapScheduledJob)
            .single();
    DraftRecord draft =
        jdbcClient.sql(
                """
                SELECT id, workspace_id, author_user_id, title, body, lifecycle_status, primary_timezone,
                       approval_route_id, campaign_label, scheduled_summary, metadata, created_at, updated_at, last_saved_at
                FROM post_drafts
                WHERE id = :draftId
                """)
            .param("draftId", job.draftId())
            .query(this::mapDraft)
            .single();
    ApprovalRequestRecord approvalRequest = findApprovalRequest(draft.id()).orElse(null);
    return new DispatchBundle(
        draft,
        findDraftAssets(draft.id()),
        findVariants(draft.id()),
        approvalRequest,
        approvalRequest == null ? List.of() : findApprovalDecisions(approvalRequest.id()),
        job);
  }

  private Map<UUID, List<UUID>> findVariantTargets(UUID draftId) {
    List<Map.Entry<UUID, UUID>> rows =
        jdbcClient.sql(
                """
                SELECT vt.variant_id, vt.connected_account_id
                FROM variant_targets vt
                JOIN post_variants pv ON pv.id = vt.variant_id
                WHERE pv.draft_id = :draftId
                ORDER BY vt.connected_account_id ASC
                """)
            .param("draftId", draftId)
            .query(
                (resultSet, rowNum) ->
                    Map.entry(
                        resultSet.getObject("variant_id", UUID.class),
                        resultSet.getObject("connected_account_id", UUID.class)))
            .list();
    Map<UUID, List<UUID>> targets = new LinkedHashMap<>();
    for (Map.Entry<UUID, UUID> row : rows) {
      targets.computeIfAbsent(row.getKey(), ignored -> new ArrayList<>()).add(row.getValue());
    }
    return targets;
  }

  private DraftSummaryRecord mapDraftSummary(ResultSet resultSet, int rowNum) throws SQLException {
    return new DraftSummaryRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("workspace_id", UUID.class),
        resultSet.getObject("author_user_id", UUID.class),
        resultSet.getString("title"),
        resultSet.getString("body"),
        resultSet.getString("lifecycle_status"),
        resultSet.getString("primary_timezone"),
        resultSet.getObject("approval_route_id", UUID.class),
        resultSet.getString("campaign_label"),
        readStringMap(resultSet.getString("scheduled_summary")),
        readStringMap(resultSet.getString("metadata")),
        toInstant(resultSet.getTimestamp("created_at")),
        toInstant(resultSet.getTimestamp("updated_at")),
        toInstant(resultSet.getTimestamp("last_saved_at")),
        resultSet.getObject("scheduled_job_id", UUID.class),
        toInstant(resultSet.getTimestamp("scheduled_for")),
        resultSet.getString("schedule_status"));
  }

  private DraftRecord mapDraft(ResultSet resultSet, int rowNum) throws SQLException {
    return new DraftRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("workspace_id", UUID.class),
        resultSet.getObject("author_user_id", UUID.class),
        resultSet.getString("title"),
        resultSet.getString("body"),
        resultSet.getString("lifecycle_status"),
        resultSet.getString("primary_timezone"),
        resultSet.getObject("approval_route_id", UUID.class),
        resultSet.getString("campaign_label"),
        readStringMap(resultSet.getString("scheduled_summary")),
        readStringMap(resultSet.getString("metadata")),
        toInstant(resultSet.getTimestamp("created_at")),
        toInstant(resultSet.getTimestamp("updated_at")),
        toInstant(resultSet.getTimestamp("last_saved_at")));
  }

  private MediaAssetRecord mapMediaAsset(ResultSet resultSet, int rowNum) throws SQLException {
    return new MediaAssetRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("workspace_id", UUID.class),
        resultSet.getString("storage_provider"),
        resultSet.getString("bucket_name"),
        resultSet.getString("object_key"),
        resultSet.getString("mime_type"),
        resultSet.getString("media_kind"),
        resultSet.getLong("size_bytes"),
        resultSet.getString("sha256_checksum"),
        resultSet.getObject("uploaded_by_user_id", UUID.class),
        resultSet.getString("alt_text"),
        resultSet.getString("source_url"),
        toInstant(resultSet.getTimestamp("created_at")));
  }

  private VariantRecord mapVariant(ResultSet resultSet, int rowNum) throws SQLException {
    return new VariantRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("draft_id", UUID.class),
        resultSet.getString("provider"),
        resultSet.getString("caption"),
        resultSet.getString("link_url"),
        resultSet.getString("first_comment"),
        readStringMap(resultSet.getString("provider_options")),
        List.of(),
        toInstant(resultSet.getTimestamp("created_at")));
  }

  private ApprovalRequestRecord mapApprovalRequest(ResultSet resultSet, int rowNum) throws SQLException {
    return new ApprovalRequestRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("draft_id", UUID.class),
        resultSet.getObject("approval_route_id", UUID.class),
        resultSet.getString("status"),
        resultSet.getObject("requested_by_user_id", UUID.class),
        toInstant(resultSet.getTimestamp("requested_at")),
        toInstant(resultSet.getTimestamp("resolved_at")));
  }

  private ApprovalDecisionRecord mapApprovalDecision(ResultSet resultSet, int rowNum) throws SQLException {
    return new ApprovalDecisionRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("approval_request_id", UUID.class),
        resultSet.getInt("step_number"),
        resultSet.getString("decision"),
        resultSet.getObject("acted_by_user_id", UUID.class),
        resultSet.getString("comment_text"),
        toInstant(resultSet.getTimestamp("acted_at")));
  }

  private ScheduledJobRecord mapScheduledJob(ResultSet resultSet, int rowNum) throws SQLException {
    return new ScheduledJobRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("draft_id", UUID.class),
        toInstant(resultSet.getTimestamp("scheduled_for")),
        resultSet.getString("timezone"),
        resultSet.getString("status"),
        resultSet.getInt("retry_count"),
        toInstant(resultSet.getTimestamp("next_attempt_at")),
        resultSet.getString("last_error_code"),
        resultSet.getString("last_error_message"),
        toInstant(resultSet.getTimestamp("created_at")),
        toInstant(resultSet.getTimestamp("locked_at")),
        resultSet.getString("locked_by"),
        toInstant(resultSet.getTimestamp("dispatched_at")),
        toInstant(resultSet.getTimestamp("published_at")),
        toInstant(resultSet.getTimestamp("cancelled_at")));
  }

  private CalendarEntryRecord mapCalendarEntry(ResultSet resultSet, int rowNum) throws SQLException {
    return new CalendarEntryRecord(
        resultSet.getObject("scheduled_job_id", UUID.class),
        resultSet.getObject("draft_id", UUID.class),
        resultSet.getString("title"),
        resultSet.getString("campaign_label"),
        resultSet.getString("lifecycle_status"),
        resultSet.getString("approval_status"),
        resultSet.getString("job_status"),
        toInstant(resultSet.getTimestamp("scheduled_for")),
        resultSet.getString("timezone"),
        readStringMap(resultSet.getString("scheduled_summary")));
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? Map.of() : value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize JSON payload", exception);
    }
  }

  private Map<String, Object> readStringMap(String value) {
    try {
      return value == null ? Map.of() : objectMapper.readValue(value, STRING_MAP);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to deserialize JSON payload", exception);
    }
  }

  private Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  private Instant toInstant(Timestamp value) {
    return value == null ? null : value.toInstant();
  }

  public record DraftSummaryRecord(
      UUID id,
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
      Instant createdAt,
      Instant updatedAt,
      Instant lastSavedAt,
      UUID scheduledJobId,
      Instant scheduledFor,
      String scheduleStatus) {}

  public record DraftRecord(
      UUID id,
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
      Instant createdAt,
      Instant updatedAt,
      Instant lastSavedAt) {}

  public record MediaAssetRecord(
      UUID id,
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

  public record VariantRecord(
      UUID id,
      UUID draftId,
      String provider,
      String caption,
      String linkUrl,
      String firstComment,
      Map<String, Object> providerOptions,
      List<UUID> targetAccountIds,
      Instant createdAt) {}

  public record ApprovalRequestRecord(
      UUID id,
      UUID draftId,
      UUID approvalRouteId,
      String status,
      UUID requestedByUserId,
      Instant requestedAt,
      Instant resolvedAt) {}

  public record ApprovalDecisionRecord(
      UUID id,
      UUID approvalRequestId,
      int stepNumber,
      String decision,
      UUID actedByUserId,
      String commentText,
      Instant actedAt) {}

  public record ScheduledJobRecord(
      UUID id,
      UUID draftId,
      Instant scheduledFor,
      String timezone,
      String status,
      int retryCount,
      Instant nextAttemptAt,
      String lastErrorCode,
      String lastErrorMessage,
      Instant createdAt,
      Instant lockedAt,
      String lockedBy,
      Instant dispatchedAt,
      Instant publishedAt,
      Instant cancelledAt) {}

  public record CalendarEntryRecord(
      UUID scheduledJobId,
      UUID draftId,
      String title,
      String campaignLabel,
      String lifecycleStatus,
      String approvalStatus,
      String jobStatus,
      Instant scheduledFor,
      String timezone,
      Map<String, Object> scheduledSummary) {}

  public record DispatchBundle(
      DraftRecord draft,
      List<MediaAssetRecord> assets,
      List<VariantRecord> variants,
      ApprovalRequestRecord approvalRequest,
      List<ApprovalDecisionRecord> approvalDecisions,
      ScheduledJobRecord scheduledJob) {}
}
