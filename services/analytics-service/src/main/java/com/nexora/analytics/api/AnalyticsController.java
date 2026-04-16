package com.nexora.analytics.api;

import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.auth.RequireScopes;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import com.nexora.analytics.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  public AnalyticsController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping("/{workspaceId}/overview")
  @RequireScopes("posts.create")
  public AnalyticsService.OverviewView getOverview(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return analyticsService.getOverview(workspaceId, from, to);
  }

  @GetMapping("/{workspaceId}/time-series")
  @RequireScopes("posts.create")
  public List<AnalyticsService.AccountMetricView> getTimeSeries(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return analyticsService.getTimeSeries(workspaceId, from, to);
  }

  @GetMapping("/{workspaceId}/top-content")
  @RequireScopes("posts.create")
  public List<AnalyticsService.ContentMetricView> getTopContent(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(value = "limit", defaultValue = "10") int limit,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return analyticsService.getTopContent(workspaceId, from, to, limit);
  }

  @GetMapping("/{workspaceId}/platform-breakdown")
  @RequireScopes("posts.create")
  public List<AnalyticsService.PlatformBreakdownView> getPlatformBreakdown(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return analyticsService.getPlatformBreakdown(workspaceId, from, to);
  }

  private void ensureWorkspaceContext(UUID workspaceId, HttpServletRequest request) {
    NexoraRequestContext ctx =
        (NexoraRequestContext) request.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);
    if (ctx.workspaceId() == null || !workspaceId.toString().equals(ctx.workspaceId())) {
      throw new ForbiddenException(
          "The authenticated workspace context does not match the requested workspace");
    }
  }
}
