package com.nexora.user.api;

import com.nexora.platform.core.auth.RequireAuthenticated;
import com.nexora.platform.core.auth.RequireScopes;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import com.nexora.user.domain.WorkspaceAccessSummary;
import com.nexora.user.domain.WorkspaceMemberSummary;
import com.nexora.user.service.UserWorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserWorkspaceController {

  private final UserWorkspaceService userWorkspaceService;

  public UserWorkspaceController(UserWorkspaceService userWorkspaceService) {
    this.userWorkspaceService = userWorkspaceService;
  }

  @GetMapping("/workspaces")
  @RequireAuthenticated
  public List<WorkspaceAccessResponse> listWorkspaces(HttpServletRequest request) {
    return userWorkspaceService.listWorkspaceAccess(currentUserId(request)).stream()
        .map(WorkspaceAccessResponse::fromDomain)
        .toList();
  }

  @GetMapping("/users/me/access")
  @RequireAuthenticated
  public List<WorkspaceAccessResponse> currentUserAccess(HttpServletRequest request) {
    return listWorkspaces(request);
  }

  @GetMapping("/workspaces/{workspaceId}/members")
  @RequireScopes("workspace.members.read")
  public List<WorkspaceMemberResponse> listMembers(
      @PathVariable("workspaceId") UUID workspaceId,
      HttpServletRequest request) {
    return userWorkspaceService.listMembers(currentUserId(request), workspaceId).stream()
        .map(WorkspaceMemberResponse::fromDomain)
        .toList();
  }

  @PostMapping("/workspaces/{workspaceId}/invites")
  @RequireScopes("workspace.members.invite")
  public WorkspaceInviteResponse createInvite(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody CreateWorkspaceInviteRequest request,
      HttpServletRequest httpServletRequest) {
    UserWorkspaceService.InviteCreationResult result =
        userWorkspaceService.createInvite(
            currentUserId(httpServletRequest),
            workspaceId,
            request.email(),
            request.roleCode());

    return new WorkspaceInviteResponse(
        result.inviteId(),
        request.email(),
        result.roleCode(),
        result.expiresAt(),
        result.inviteToken());
  }

  private UUID currentUserId(HttpServletRequest request) {
    NexoraRequestContext requestContext =
        (NexoraRequestContext) request.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);
    return UUID.fromString(requestContext.userId());
  }

  public record CreateWorkspaceInviteRequest(
      @Email @NotBlank String email,
      @NotBlank String roleCode) {}

  public record WorkspaceInviteResponse(
      UUID inviteId,
      String email,
      String roleCode,
      java.time.Instant expiresAt,
      String inviteToken) {}

  public record WorkspaceAccessResponse(
      UUID membershipId,
      UUID workspaceId,
      String workspaceName,
      String workspaceSlug,
      String workspaceStatus,
      String roleCode,
      String roleName,
      String membershipStatus,
      java.time.Instant joinedAt,
      java.util.Set<String> permissions) {
    static WorkspaceAccessResponse fromDomain(WorkspaceAccessSummary summary) {
      return new WorkspaceAccessResponse(
          summary.membershipId(),
          summary.workspaceId(),
          summary.workspaceName(),
          summary.workspaceSlug(),
          summary.workspaceStatus(),
          summary.roleCode(),
          summary.roleName(),
          summary.membershipStatus(),
          summary.joinedAt(),
          summary.permissions());
    }
  }

  public record WorkspaceMemberResponse(
      UUID membershipId,
      UUID workspaceId,
      UUID userId,
      String roleCode,
      String roleName,
      String status,
      java.time.Instant joinedAt) {
    static WorkspaceMemberResponse fromDomain(WorkspaceMemberSummary summary) {
      return new WorkspaceMemberResponse(
          summary.membershipId(),
          summary.workspaceId(),
          summary.userId(),
          summary.roleCode(),
          summary.roleName(),
          summary.status(),
          summary.joinedAt());
    }
  }
}
