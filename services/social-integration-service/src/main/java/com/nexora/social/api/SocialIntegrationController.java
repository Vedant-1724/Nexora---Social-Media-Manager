package com.nexora.social.api;

import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.auth.RequireScopes;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import com.nexora.social.provider.SocialProvider;
import com.nexora.social.provider.SocialProviderAdapter.ProviderDescriptor;
import com.nexora.social.service.SocialIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SocialIntegrationController {

  private final SocialIntegrationService socialIntegrationService;

  public SocialIntegrationController(SocialIntegrationService socialIntegrationService) {
    this.socialIntegrationService = socialIntegrationService;
  }

  @GetMapping("/workspaces/{workspaceId}/social/providers")
  @RequireScopes("workspace.read")
  public List<ProviderDescriptor> listProviders(
      @PathVariable("workspaceId") UUID workspaceId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return socialIntegrationService.listProviders();
  }

  @PostMapping("/workspaces/{workspaceId}/social/link-sessions")
  @RequireScopes("workspace.manage")
  public SocialIntegrationService.LinkSessionView createLinkSession(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody CreateLinkSessionRequest request,
      HttpServletRequest httpServletRequest) {
    NexoraRequestContext requestContext = requestContext(httpServletRequest);
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return socialIntegrationService.createLinkSession(
        UUID.fromString(requestContext.userId()),
        workspaceId,
        SocialProvider.fromCode(request.provider()),
        request.scopes());
  }

  @GetMapping("/workspaces/{workspaceId}/social/accounts")
  @RequireScopes("workspace.read")
  public List<SocialIntegrationService.AccountView> listConnectedAccounts(
      @PathVariable("workspaceId") UUID workspaceId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return socialIntegrationService.listConnectedAccounts(workspaceId);
  }

  @PostMapping("/workspaces/{workspaceId}/social/accounts/{connectedAccountId}/refresh")
  @RequireScopes("workspace.manage")
  public SocialIntegrationService.AccountView refreshAccountTokens(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("connectedAccountId") UUID connectedAccountId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return socialIntegrationService.refreshAccountTokens(workspaceId, connectedAccountId);
  }

  @DeleteMapping("/workspaces/{workspaceId}/social/accounts/{connectedAccountId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequireScopes("workspace.manage")
  public void revokeAccount(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("connectedAccountId") UUID connectedAccountId,
      HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    socialIntegrationService.revokeAccount(workspaceId, connectedAccountId);
  }

  @PostMapping("/workspaces/{workspaceId}/social/publications")
  @RequireScopes("posts.create")
  public SocialIntegrationService.PublicationView publish(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody PublishRequest request,
      HttpServletRequest httpServletRequest) {
    ensureWorkspaceContext(workspaceId, httpServletRequest);
    return socialIntegrationService.publish(
        workspaceId,
        new SocialIntegrationService.PublishCommand(
            request.connectedAccountId(),
            request.message(),
            request.linkUrl(),
            request.mediaUrls() == null ? List.of() : request.mediaUrls(),
            request.replyToExternalPostId(),
            request.metadata() == null ? Map.of() : request.metadata()));
  }

  @GetMapping("/social/oauth/{provider}/callback")
  public SocialIntegrationService.OAuthCallbackView completeOAuthCallback(
      @PathVariable("provider") String provider,
      @RequestParam(name = "code", required = false) String code,
      @RequestParam(name = "state", required = false) String state,
      @RequestParam(name = "error", required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription) {
    if (error != null) {
      throw new IllegalArgumentException(errorDescription == null ? error : errorDescription);
    }
    if (code == null || state == null) {
      throw new IllegalArgumentException("OAuth callback requires both code and state");
    }
    return socialIntegrationService.completeOAuthCallback(SocialProvider.fromCode(provider), code, state);
  }

  private void ensureWorkspaceContext(UUID workspaceId, HttpServletRequest request) {
    NexoraRequestContext requestContext = requestContext(request);
    if (requestContext.workspaceId() == null || !workspaceId.toString().equals(requestContext.workspaceId())) {
      throw new ForbiddenException(
          "The authenticated workspace context does not match the requested workspace");
    }
  }

  private NexoraRequestContext requestContext(HttpServletRequest request) {
    return (NexoraRequestContext) request.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);
  }

  public record CreateLinkSessionRequest(
      @NotBlank String provider,
      List<String> scopes) {}

  public record PublishRequest(
      UUID connectedAccountId,
      @NotBlank String message,
      String linkUrl,
      List<String> mediaUrls,
      String replyToExternalPostId,
      Map<String, Object> metadata) {}
}
