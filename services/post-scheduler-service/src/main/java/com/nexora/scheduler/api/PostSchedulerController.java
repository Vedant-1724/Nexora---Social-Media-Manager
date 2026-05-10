package com.nexora.scheduler.api;

import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.auth.RequireScopes;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import com.nexora.scheduler.service.PostSchedulerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1")
public class PostSchedulerController {

  private final PostSchedulerService postSchedulerService;

  public PostSchedulerController(PostSchedulerService postSchedulerService) {
    this.postSchedulerService = postSchedulerService;
  }

  @GetMapping("/workspaces/{workspaceId}/posts/drafts")
  @RequireScopes("posts.create")
  public List<PostSchedulerService.DraftSummaryView> listDrafts(
      @PathVariable("workspaceId") UUID workspaceId, HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return postSchedulerService.listDrafts(workspaceId);
  }

  @GetMapping("/workspaces/{workspaceId}/posts/drafts/{draftId}")
  @RequireScopes("posts.create")
  public PostSchedulerService.DraftDetailsView getDraft(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("draftId") UUID draftId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return postSchedulerService.getDraft(workspaceId, draftId);
  }

  @PostMapping("/workspaces/{workspaceId}/posts/drafts")
  @RequireScopes("posts.create")
  public PostSchedulerService.DraftDetailsView createDraft(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody SaveDraftRequest request,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return postSchedulerService.saveDraft(
        currentUserId(httpServletRequest),
        workspaceId,
        toSaveCommand(null, request));
  }

  @PutMapping("/workspaces/{workspaceId}/posts/drafts/{draftId}")
  @RequireScopes("posts.create")
  public PostSchedulerService.DraftDetailsView updateDraft(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("draftId") UUID draftId,
      @Valid @RequestBody SaveDraftRequest request,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return postSchedulerService.saveDraft(
        currentUserId(httpServletRequest),
        workspaceId,
        toSaveCommand(draftId, request));
  }

  @PostMapping("/workspaces/{workspaceId}/posts/drafts/{draftId}/approval-requests")
  @RequireScopes("posts.create")
  public PostSchedulerService.ApprovalRequestView requestApproval(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("draftId") UUID draftId,
      @Valid @RequestBody ApprovalSubmissionRequest request,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return postSchedulerService.requestApproval(
        currentUserId(httpServletRequest),
        workspaceId,
        draftId,
        new PostSchedulerService.ApprovalSubmission(request.approvalRouteId(), request.note()));
  }

  @PostMapping("/workspaces/{workspaceId}/posts/drafts/{draftId}/approval-decisions")
  @RequireScopes("posts.approve")
  public PostSchedulerService.ApprovalRequestView recordApprovalDecision(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("draftId") UUID draftId,
      @Valid @RequestBody ApprovalDecisionRequest request,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return postSchedulerService.recordApprovalDecision(
        currentUserId(httpServletRequest),
        workspaceId,
        draftId,
        new PostSchedulerService.ApprovalDecisionCommand(
            request.stepNumber(), request.decision(), request.commentText()));
  }

  @PostMapping("/workspaces/{workspaceId}/posts/drafts/{draftId}/schedule")
  @RequireScopes("posts.create")
  public PostSchedulerService.ScheduledJobView scheduleDraft(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("draftId") UUID draftId,
      @Valid @RequestBody ScheduleDraftRequest request,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return postSchedulerService.scheduleDraft(
        currentUserId(httpServletRequest),
        workspaceId,
        draftId,
        new PostSchedulerService.ScheduleCommand(request.scheduledFor(), request.timezone()));
  }

  @DeleteMapping("/workspaces/{workspaceId}/posts/drafts/{draftId}/schedule")
  @RequireScopes("posts.create")
  public void cancelSchedule(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("draftId") UUID draftId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    postSchedulerService.cancelSchedule(workspaceId, draftId);
  }

  @GetMapping("/workspaces/{workspaceId}/calendar/posts")
  @RequireScopes("posts.create")
  public List<PostSchedulerService.CalendarEntryView> listCalendarEntries(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return postSchedulerService.listCalendarEntries(workspaceId, from, to);
  }

  @PostMapping("/workspaces/{workspaceId}/posts/jobs/{jobId}/dispatch")
  @RequireScopes("posts.create")
  public PostSchedulerService.DispatchResult dispatchJob(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("jobId") UUID jobId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return postSchedulerService.dispatchScheduledJob(workspaceId, jobId, currentUserId(request));
  }

  // ── Bulk Import ─────────────────────────────────────────────────────────────

  @PostMapping(value = "/workspaces/{workspaceId}/posts/bulk-import", consumes = "multipart/form-data")
  @RequireScopes("posts.create")
  public PostSchedulerService.BulkImportResult bulkImport(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam("file") MultipartFile file,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    List<Map<String, String>> csvRows = parseCsv(file);
    if (csvRows.isEmpty()) {
      throw new IllegalArgumentException("CSV file is empty or has no data rows");
    }
    if (csvRows.size() > 350) {
      throw new IllegalArgumentException("CSV file exceeds the 350-post limit per bulk import");
    }
    return postSchedulerService.initiateBulkImport(
        currentUserId(httpServletRequest), workspaceId, csvRows);
  }

  // ── Link-in-Bio Endpoints ──────────────────────────────────────────────────

  @PostMapping("/workspaces/{workspaceId}/bio-pages")
  @RequireScopes("posts.create")
  public PostSchedulerService.BioPageView createBioPage(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody CreateBioPageRequest request,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return postSchedulerService.createBioPage(
        workspaceId,
        new PostSchedulerService.CreateBioPageCommand(
            request.slug(), request.title(), request.bioText(),
            request.avatarUrl(), request.themeConfig()));
  }

  @GetMapping("/workspaces/{workspaceId}/bio-pages")
  @RequireScopes("posts.create")
  public List<PostSchedulerService.BioPageView> listBioPages(
      @PathVariable("workspaceId") UUID workspaceId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return postSchedulerService.listBioPages(workspaceId);
  }

  @GetMapping("/workspaces/{workspaceId}/bio-pages/{pageId}")
  @RequireScopes("posts.create")
  public PostSchedulerService.BioPageView getBioPage(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("pageId") UUID pageId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return postSchedulerService.getBioPage(workspaceId, pageId);
  }

  @PutMapping("/workspaces/{workspaceId}/bio-pages/{pageId}")
  @RequireScopes("posts.create")
  public PostSchedulerService.BioPageView updateBioPage(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("pageId") UUID pageId,
      @Valid @RequestBody UpdateBioPageRequest request,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return postSchedulerService.updateBioPage(
        workspaceId, pageId,
        new PostSchedulerService.UpdateBioPageCommand(
            request.title(), request.bioText(), request.avatarUrl(), request.themeConfig()));
  }

  @PostMapping("/workspaces/{workspaceId}/bio-pages/{pageId}/entries")
  @RequireScopes("posts.create")
  public PostSchedulerService.BioEntryView addBioEntry(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("pageId") UUID pageId,
      @Valid @RequestBody AddBioEntryRequest request,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return postSchedulerService.addBioEntry(
        workspaceId, pageId,
        new PostSchedulerService.AddBioEntryCommand(
            request.draftId(), request.externalUrl(), request.thumbnailUrl(),
            request.label(), request.sortOrder(), request.isPinned()));
  }

  @DeleteMapping("/workspaces/{workspaceId}/bio-pages/{pageId}/entries/{entryId}")
  @RequireScopes("posts.create")
  public void removeBioEntry(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("pageId") UUID pageId,
      @PathVariable("entryId") UUID entryId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    postSchedulerService.removeBioEntry(workspaceId, pageId, entryId);
  }

  // ── CSV Parser ─────────────────────────────────────────────────────────────

  private List<Map<String, String>> parseCsv(MultipartFile file) {
    List<Map<String, String>> rows = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String headerLine = reader.readLine();
      if (headerLine == null || headerLine.isBlank()) {
        return rows;
      }
      String[] headers = headerLine.split(",", -1);
      for (int i = 0; i < headers.length; i++) {
        headers[i] = headers[i].trim().replaceAll("^\"|\"$", "");
      }
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) continue;
        String[] values = line.split(",", -1);
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
          String value = i < values.length ? values[i].trim().replaceAll("^\"|\"$", "") : "";
          row.put(headers[i], value);
        }
        rows.add(row);
      }
    } catch (Exception exception) {
      throw new IllegalArgumentException("Failed to parse CSV: " + exception.getMessage(), exception);
    }
    return rows;
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private PostSchedulerService.SaveDraftCommand toSaveCommand(UUID draftId, SaveDraftRequest request) {
    return new PostSchedulerService.SaveDraftCommand(
        draftId,
        request.title(),
        request.body(),
        request.primaryTimezone(),
        request.approvalRouteId(),
        request.campaignLabel(),
        request.metadata(),
        request.mediaAssets() == null
            ? List.of()
            : request.mediaAssets().stream()
                .map(
                    asset ->
                        new PostSchedulerService.MediaAssetInput(
                            asset.assetId(),
                            asset.storageProvider(),
                            asset.bucketName(),
                            asset.objectKey(),
                            asset.mimeType(),
                            asset.mediaKind(),
                            asset.sizeBytes(),
                            asset.sha256Checksum(),
                            asset.altText(),
                            asset.sourceUrl()))
                .toList(),
        request.variants() == null
            ? List.of()
            : request.variants().stream()
                .map(
                    variant ->
                        new PostSchedulerService.VariantInput(
                            variant.provider(),
                            variant.caption(),
                            variant.linkUrl(),
                            variant.firstComment(),
                            variant.providerOptions(),
                            variant.targetAccountIds()))
                .toList());
  }

  private UUID currentUserId(HttpServletRequest request) {
    return UUID.fromString(requestContext(request).userId());
  }

  private void ensureWorkspaceContext(UUID workspaceId, HttpServletRequest request) {
    NexoraRequestContext requestContext = requestContext(request);
    if (requestContext.workspaceId() == null || !workspaceId.toString().equals(requestContext.workspaceId())) {
      throw new ForbiddenException("The authenticated workspace context does not match the requested workspace");
    }
  }

  private NexoraRequestContext requestContext(HttpServletRequest request) {
    return (NexoraRequestContext) request.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);
  }

  // ── Request Records ────────────────────────────────────────────────────────

  public record SaveDraftRequest(
      @NotBlank String title,
      @NotBlank String body,
      @NotBlank String primaryTimezone,
      UUID approvalRouteId,
      String campaignLabel,
      Map<String, Object> metadata,
      List<@Valid MediaAssetRequest> mediaAssets,
      @NotEmpty List<@Valid VariantRequest> variants) {}

  public record MediaAssetRequest(
      UUID assetId,
      String storageProvider,
      @NotBlank String bucketName,
      @NotBlank String objectKey,
      @NotBlank String mimeType,
      @NotBlank String mediaKind,
      @Positive long sizeBytes,
      @NotBlank String sha256Checksum,
      String altText,
      String sourceUrl) {}

  public record VariantRequest(
      @NotBlank String provider,
      @NotBlank String caption,
      String linkUrl,
      String firstComment,
      Map<String, Object> providerOptions,
      List<UUID> targetAccountIds) {}

  public record ApprovalSubmissionRequest(UUID approvalRouteId, String note) {}

  public record ApprovalDecisionRequest(
      @Positive int stepNumber,
      @NotBlank String decision,
      String commentText) {}

  public record ScheduleDraftRequest(@NotNull @Future Instant scheduledFor, @NotBlank String timezone) {}

  public record CreateBioPageRequest(
      @NotBlank String slug,
      @NotBlank String title,
      String bioText,
      String avatarUrl,
      Map<String, Object> themeConfig) {}

  public record UpdateBioPageRequest(
      @NotBlank String title,
      String bioText,
      String avatarUrl,
      Map<String, Object> themeConfig) {}

  public record AddBioEntryRequest(
      UUID draftId,
      String externalUrl,
      String thumbnailUrl,
      @NotBlank String label,
      int sortOrder,
      boolean isPinned) {}


