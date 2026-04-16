package com.nexora.auth.service;

import com.nexora.auth.config.AuthSecurityProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthUserAccessClient {

  private final RestClient restClient;
  private final AuthSecurityProperties authSecurityProperties;

  public AuthUserAccessClient(RestClient restClient, AuthSecurityProperties authSecurityProperties) {
    this.restClient = restClient;
    this.authSecurityProperties = authSecurityProperties;
  }

  public WorkspaceAccessResponse bootstrapWorkspace(
      UUID userId,
      String email,
      String displayName,
      String workspaceName,
      String locale,
      String timezone) {
    return restClient.post()
        .uri(authSecurityProperties.getUserServiceBaseUrl() + "/api/v1/internal/workspaces/bootstrap")
        .body(new BootstrapWorkspaceRequest(userId, email, displayName, workspaceName, locale, timezone))
        .retrieve()
        .body(WorkspaceAccessResponse.class);
  }

  public List<WorkspaceAccessResponse> listWorkspaceAccess(UUID userId) {
    return restClient.get()
        .uri(authSecurityProperties.getUserServiceBaseUrl() + "/api/v1/internal/users/{userId}/workspace-access", userId)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public WorkspaceAccessResponse acceptInvite(String inviteToken, UUID userId, String email) {
    return restClient.post()
        .uri(authSecurityProperties.getUserServiceBaseUrl() + "/api/v1/internal/workspace-invites/accept")
        .body(new AcceptInviteRequest(inviteToken, userId, email))
        .retrieve()
        .body(WorkspaceAccessResponse.class);
  }

  public record BootstrapWorkspaceRequest(
      UUID userId,
      String email,
      String displayName,
      String workspaceName,
      String locale,
      String timezone) {}

  public record AcceptInviteRequest(
      @NotBlank String inviteToken,
      UUID userId,
      @Email String email) {}

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
      Set<String> permissions) {}
}
