package com.nexora.analytics.service;

import com.nexora.analytics.repository.AnalyticsRepository;
import com.nexora.analytics.repository.AnalyticsRepository.AccountDailyMetricRecord;
import com.nexora.analytics.repository.AnalyticsRepository.ContentDailyMetricRecord;
import com.nexora.analytics.repository.AnalyticsRepository.OverviewTotals;
import com.nexora.analytics.repository.AnalyticsRepository.PlatformTotals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

  private final AnalyticsRepository analyticsRepository;

  public AnalyticsService(AnalyticsRepository analyticsRepository) {
    this.analyticsRepository = analyticsRepository;
  }

  public OverviewView getOverview(UUID workspaceId, LocalDate from, LocalDate to) {
    OverviewTotals current = analyticsRepository.aggregateOverview(workspaceId, from, to);

    long periodDays = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
    LocalDate prevFrom = from.minusDays(periodDays);
    LocalDate prevTo = from.minusDays(1);
    OverviewTotals previous = analyticsRepository.aggregateOverview(workspaceId, prevFrom, prevTo);

    return new OverviewView(
        current.impressions(),
        current.reach(),
        current.engagements(),
        current.comments(),
        current.clicks(),
        current.followerDelta(),
        computeEngagementRate(current.engagements(), current.impressions()),
        changePercent(current.impressions(), previous.impressions()),
        changePercent(current.engagements(), previous.engagements()),
        changePercent(current.reach(), previous.reach()),
        current.followerDelta() - previous.followerDelta());
  }

  public List<AccountMetricView> getTimeSeries(UUID workspaceId, LocalDate from, LocalDate to) {
    List<AccountDailyMetricRecord> records =
        analyticsRepository.findAccountDailyMetrics(workspaceId, from, to);

    return records.stream()
        .map(r -> new AccountMetricView(
            r.metricDate().toString(),
            r.provider(),
            r.impressions(),
            r.reach(),
            r.engagements(),
            r.comments(),
            r.clicks(),
            r.followerDelta()))
        .toList();
  }

  public List<ContentMetricView> getTopContent(UUID workspaceId, LocalDate from, LocalDate to, int limit) {
    List<ContentDailyMetricRecord> records =
        analyticsRepository.findTopContent(workspaceId, from, to, limit);

    return records.stream()
        .map(r -> new ContentMetricView(
            r.draftId().toString(),
            r.provider(),
            r.providerPostId(),
            r.impressions(),
            r.likes(),
            r.comments(),
            r.shares(),
            r.clicks(),
            r.saves(),
            r.videoViews(),
            computeEngagementRate(
                r.likes() + r.comments() + r.shares() + r.clicks(),
                r.impressions())))
        .toList();
  }

  public List<PlatformBreakdownView> getPlatformBreakdown(UUID workspaceId, LocalDate from, LocalDate to) {
    List<PlatformTotals> totals = analyticsRepository.aggregateByPlatform(workspaceId, from, to);

    long grandTotal = totals.stream().mapToLong(PlatformTotals::impressions).sum();
    if (grandTotal == 0) {
      return totals.stream()
          .map(t -> new PlatformBreakdownView(t.provider(), t.impressions(), t.engagements(), 0.0))
          .toList();
    }

    return totals.stream()
        .map(t -> new PlatformBreakdownView(
            t.provider(),
            t.impressions(),
            t.engagements(),
            BigDecimal.valueOf(t.impressions() * 100.0 / grandTotal)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue()))
        .toList();
  }

  private double computeEngagementRate(long engagements, long impressions) {
    if (impressions == 0) return 0.0;
    return BigDecimal.valueOf(engagements * 100.0 / impressions)
        .setScale(2, RoundingMode.HALF_UP)
        .doubleValue();
  }

  private double changePercent(long current, long previous) {
    if (previous == 0) return current > 0 ? 100.0 : 0.0;
    return BigDecimal.valueOf((current - previous) * 100.0 / previous)
        .setScale(1, RoundingMode.HALF_UP)
        .doubleValue();
  }

  // ── View Records ───────────────────────────────────────────────────────────

  public record OverviewView(
      long impressions, long reach, long engagements, long comments, long clicks,
      int followerDelta, double engagementRate,
      double impressionsChange, double engagementsChange,
      double reachChange, int followerDeltaChange) {}

  public record AccountMetricView(
      String date, String provider, long impressions, long reach,
      long engagements, long comments, long clicks, int followerDelta) {}

  public record ContentMetricView(
      String draftId, String provider, String providerPostId,
      long impressions, long likes, long comments, long shares,
      long clicks, long saves, long videoViews, double engagementRate) {}

  public record PlatformBreakdownView(
      String provider, long impressions, long engagements, double percentage) {}
}
