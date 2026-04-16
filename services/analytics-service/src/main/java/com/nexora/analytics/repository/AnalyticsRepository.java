package com.nexora.analytics.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsRepository {

  private static final TypeReference<Map<String, Object>> JSON_MAP = new TypeReference<>() {};

  private final JdbcClient jdbcClient;
  private final ObjectMapper objectMapper;

  public AnalyticsRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
    this.jdbcClient = jdbcClient;
    this.objectMapper = objectMapper;
  }

  // ── Account Daily Metrics ──────────────────────────────────────────────────

  public List<AccountDailyMetricRecord> findAccountDailyMetrics(
      UUID workspaceId, LocalDate from, LocalDate to) {
    return jdbcClient.sql(
            """
            SELECT id, workspace_id, connected_account_id, provider, metric_date,
                   impressions, reach, engagements, comments, clicks, follower_delta
            FROM account_daily_metrics
            WHERE workspace_id = :workspaceId
              AND metric_date >= :fromDate
              AND metric_date <= :toDate
            ORDER BY metric_date ASC, provider ASC
            """)
        .param("workspaceId", workspaceId)
        .param("fromDate", from)
        .param("toDate", to)
        .query(this::mapAccountDailyMetric)
        .list();
  }

  public OverviewTotals aggregateOverview(UUID workspaceId, LocalDate from, LocalDate to) {
    return jdbcClient.sql(
            """
            SELECT COALESCE(SUM(impressions), 0) AS total_impressions,
                   COALESCE(SUM(reach), 0) AS total_reach,
                   COALESCE(SUM(engagements), 0) AS total_engagements,
                   COALESCE(SUM(comments), 0) AS total_comments,
                   COALESCE(SUM(clicks), 0) AS total_clicks,
                   COALESCE(SUM(follower_delta), 0) AS total_follower_delta
            FROM account_daily_metrics
            WHERE workspace_id = :workspaceId
              AND metric_date >= :fromDate
              AND metric_date <= :toDate
            """)
        .param("workspaceId", workspaceId)
        .param("fromDate", from)
        .param("toDate", to)
        .query((rs, rowNum) -> new OverviewTotals(
            rs.getLong("total_impressions"),
            rs.getLong("total_reach"),
            rs.getLong("total_engagements"),
            rs.getLong("total_comments"),
            rs.getLong("total_clicks"),
            rs.getInt("total_follower_delta")))
        .single();
  }

  public List<PlatformTotals> aggregateByPlatform(UUID workspaceId, LocalDate from, LocalDate to) {
    return jdbcClient.sql(
            """
            SELECT provider,
                   COALESCE(SUM(impressions), 0) AS total_impressions,
                   COALESCE(SUM(engagements), 0) AS total_engagements
            FROM account_daily_metrics
            WHERE workspace_id = :workspaceId
              AND metric_date >= :fromDate
              AND metric_date <= :toDate
            GROUP BY provider
            ORDER BY total_impressions DESC
            """)
        .param("workspaceId", workspaceId)
        .param("fromDate", from)
        .param("toDate", to)
        .query((rs, rowNum) -> new PlatformTotals(
            rs.getString("provider"),
            rs.getLong("total_impressions"),
            rs.getLong("total_engagements")))
        .list();
  }

  // ── Content Daily Metrics ──────────────────────────────────────────────────

  public List<ContentDailyMetricRecord> findTopContent(
      UUID workspaceId, LocalDate from, LocalDate to, int limit) {
    return jdbcClient.sql(
            """
            SELECT c.id, c.workspace_id, c.draft_id, c.provider, c.provider_post_id,
                   c.metric_date,
                   SUM(c.impressions) AS impressions,
                   SUM(c.likes) AS likes,
                   SUM(c.comments) AS comments,
                   SUM(c.shares) AS shares,
                   SUM(c.clicks) AS clicks,
                   SUM(c.saves) AS saves,
                   SUM(c.video_views) AS video_views
            FROM content_daily_metrics c
            WHERE c.workspace_id = :workspaceId
              AND c.metric_date >= :fromDate
              AND c.metric_date <= :toDate
            GROUP BY c.id, c.workspace_id, c.draft_id, c.provider, c.provider_post_id, c.metric_date
            ORDER BY impressions DESC
            LIMIT :limit
            """)
        .param("workspaceId", workspaceId)
        .param("fromDate", from)
        .param("toDate", to)
        .param("limit", limit)
        .query(this::mapContentDailyMetric)
        .list();
  }

  // ── Workspace Rollups ──────────────────────────────────────────────────────

  public Optional<WorkspaceRollupRecord> findRollup(
      UUID workspaceId, String granularity, LocalDate periodStart, LocalDate periodEnd) {
    return jdbcClient.sql(
            """
            SELECT id, workspace_id, granularity, period_start, period_end, totals, generated_at
            FROM workspace_rollups
            WHERE workspace_id = :workspaceId
              AND granularity = :granularity
              AND period_start = :periodStart
              AND period_end = :periodEnd
            """)
        .param("workspaceId", workspaceId)
        .param("granularity", granularity)
        .param("periodStart", periodStart)
        .param("periodEnd", periodEnd)
        .query(this::mapWorkspaceRollup)
        .optional();
  }

  // ── Row Mappers ────────────────────────────────────────────────────────────

  private AccountDailyMetricRecord mapAccountDailyMetric(ResultSet rs, int rowNum) throws SQLException {
    return new AccountDailyMetricRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("workspace_id", UUID.class),
        rs.getObject("connected_account_id", UUID.class),
        rs.getString("provider"),
        rs.getDate("metric_date").toLocalDate(),
        rs.getLong("impressions"),
        rs.getLong("reach"),
        rs.getLong("engagements"),
        rs.getLong("comments"),
        rs.getLong("clicks"),
        rs.getInt("follower_delta"));
  }

  private ContentDailyMetricRecord mapContentDailyMetric(ResultSet rs, int rowNum) throws SQLException {
    return new ContentDailyMetricRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("workspace_id", UUID.class),
        rs.getObject("draft_id", UUID.class),
        rs.getString("provider"),
        rs.getString("provider_post_id"),
        rs.getDate("metric_date").toLocalDate(),
        rs.getLong("impressions"),
        rs.getLong("likes"),
        rs.getLong("comments"),
        rs.getLong("shares"),
        rs.getLong("clicks"),
        rs.getLong("saves"),
        rs.getLong("video_views"));
  }

  private WorkspaceRollupRecord mapWorkspaceRollup(ResultSet rs, int rowNum) throws SQLException {
    return new WorkspaceRollupRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("workspace_id", UUID.class),
        rs.getString("granularity"),
        rs.getDate("period_start").toLocalDate(),
        rs.getDate("period_end").toLocalDate(),
        fromJson(rs.getString("totals")),
        rs.getTimestamp("generated_at").toInstant());
  }

  private Map<String, Object> fromJson(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, JSON_MAP);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse JSON: " + json, e);
    }
  }

  // ── Records ────────────────────────────────────────────────────────────────

  public record AccountDailyMetricRecord(
      UUID id, UUID workspaceId, UUID connectedAccountId, String provider,
      LocalDate metricDate, long impressions, long reach, long engagements,
      long comments, long clicks, int followerDelta) {}

  public record ContentDailyMetricRecord(
      UUID id, UUID workspaceId, UUID draftId, String provider,
      String providerPostId, LocalDate metricDate, long impressions,
      long likes, long comments, long shares, long clicks,
      long saves, long videoViews) {}

  public record WorkspaceRollupRecord(
      UUID id, UUID workspaceId, String granularity,
      LocalDate periodStart, LocalDate periodEnd,
      Map<String, Object> totals, Instant generatedAt) {}

  public record OverviewTotals(
      long impressions, long reach, long engagements,
      long comments, long clicks, int followerDelta) {}

  public record PlatformTotals(
      String provider, long impressions, long engagements) {}
}
