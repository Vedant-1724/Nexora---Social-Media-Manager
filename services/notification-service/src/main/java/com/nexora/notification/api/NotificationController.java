package com.nexora.notification.api;

import com.nexora.notification.service.NotificationService;
import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.auth.RequireScopes;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  // ── In-App Notifications ──────────────────────────────────────────────────

  @GetMapping("/workspaces/{workspaceId}/in-app")
  @RequireScopes("workspace.read")
  public List<NotificationService.NotificationView> getUnreadInApp(
      @PathVariable("workspaceId") UUID workspaceId, HttpServletRequest request) {
    NexoraRequestContext ctx = ensureWorkspaceContext(workspaceId, request);
    return notificationService.getUnreadInApp(workspaceId, UUID.fromString(ctx.userId()));
  }

  @PutMapping("/workspaces/{workspaceId}/in-app/{notificationId}/read")
  @RequireScopes("workspace.read")
  public ResponseEntity<Void> markAsRead(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("notificationId") UUID notificationId,
      HttpServletRequest request) {
    NexoraRequestContext ctx = ensureWorkspaceContext(workspaceId, request);
    notificationService.markAsRead(workspaceId, UUID.fromString(ctx.userId()), notificationId);
    return ResponseEntity.noContent().build();
  }

  // ── Preferences ───────────────────────────────────────────────────────────

  @GetMapping("/workspaces/{workspaceId}/preferences")
  @RequireScopes("workspace.read")
  public List<NotificationService.PreferenceView> getPreferences(
      @PathVariable("workspaceId") UUID workspaceId, HttpServletRequest request) {
    NexoraRequestContext ctx = ensureWorkspaceContext(workspaceId, request);
    return notificationService.getPreferences(workspaceId, UUID.fromString(ctx.userId()));
  }

  @PutMapping("/workspaces/{workspaceId}/preferences")
  @RequireScopes("workspace.read")
  public ResponseEntity<Void> updatePreference(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestBody PreferenceUpdateRequest p,
      HttpServletRequest request) {
    NexoraRequestContext ctx = ensureWorkspaceContext(workspaceId, request);
    notificationService.setPreference(
        workspaceId, UUID.fromString(ctx.userId()), p.eventCode(), p.channel(), p.enabled());
    return ResponseEntity.noContent().build();
  }

  public record PreferenceUpdateRequest(String eventCode, String channel, boolean enabled) {}

  private NexoraRequestContext ensureWorkspaceContext(UUID workspaceId, HttpServletRequest request) {
    NexoraRequestContext ctx =
        (NexoraRequestContext) request.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);
    if (ctx == null || ctx.workspaceId() == null || !workspaceId.toString().equals(ctx.workspaceId())) {
      throw new ForbiddenException("Invalid workspace context");
    }
    return ctx;
  }
}
