package com.nexora.user.api;

import com.nexora.user.domain.WorkspaceAccessSummary;
import com.nexora.user.service.UserWorkspaceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
public class UserWorkspaceInternalController {

  private final UserWorkspaceService userWorkspaceService;

  public UserWorkspaceInternalController(UserWorkspaceService userWorkspaceService) {
    this.userWorkspaceService = userWorkspaceService;
  }

  @PostMapping("/workspaces/bootstrap")
  public WorkspaceBootstrapResponse bootstrapWorkspace(
      @Valid @RequestBody WorkspaceBootstrapRequest request) {
    WorkspaceAccessSummary accessSummary =
        userWorkspaceService.bootstrapWorkspace(
            request.userId(),
            request.email(),
            request.workspaceName(),
            request.locale(),
            request.timezone());
    return WorkspaceBootstrapResponse.fromDomain(accessSummary);
  }

  @GetMapping("/users/{userId}/workspace-access")
  public List<UserWorkspaceController.WorkspaceAccessResponse> workspaceAccess(
      @PathVariable("userId") UUID userId) {
    return userWorkspaceService.listWorkspaceAccess(userId).stream()
        .map(UserWorkspaceController.WorkspaceAccessResponse::fromDomain)
        .toList();
  }

  @PostMapping("/workspace-invites/accept")
  public UserWorkspaceController.WorkspaceAccessResponse acceptInvite(
      @Valid @RequestBody AcceptWorkspaceInviteRequest request) {
    return UserWorkspaceController.WorkspaceAccessResponse.fromDomain(
        userWorkspaceService.acceptInvite(
            request.inviteToken(),
            request.userId(),
            request.email()));
  }

  public record WorkspaceBootstrapRequest(
      @NotNull UUID userId,
      @Email @NotBlank String email,
      @NotBlank String displayName,
      @NotBlank String workspaceName,
      String locale,
      String timezone) {}

  public record AcceptWorkspaceInviteRequest(
      @NotBlank String inviteToken,
      @NotNull UUID userId,
      @Email @NotBlank String email) {}

  public record WorkspaceBootstrapResponse(
      UUID membershipId,
      UUID workspaceId,
      String workspaceName,
      String workspaceSlug,
      String roleCode,
      java.util.Set<String> permissions) {
    static WorkspaceBootstrapResponse fromDomain(WorkspaceAccessSummary summary) {
      return new WorkspaceBootstrapResponse(
          summary.membershipId(),
          summary.workspaceId(),
          summary.workspaceName(),
          summary.workspaceSlug(),
          summary.roleCode(),
          summary.permissions());
    }
  }
}
