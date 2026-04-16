package com.nexora.auth.api;

import com.nexora.auth.service.AuthWorkflowService;
import com.nexora.auth.service.AuthWorkflowService.AuthSessionView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthWorkflowService authWorkflowService;

  public AuthController(AuthWorkflowService authWorkflowService) {
    this.authWorkflowService = authWorkflowService;
  }

  @PostMapping("/signup")
  public AuthSessionResponse signUp(
      @Valid @RequestBody SignUpRequest request,
      HttpServletRequest httpServletRequest) {
    return AuthSessionResponse.fromService(
        authWorkflowService.signUp(
            new AuthWorkflowService.SignUpCommand(
                request.email(),
                request.password(),
                request.displayName(),
                request.workspaceName(),
                request.locale(),
                request.timezone()),
            clientContext(httpServletRequest)));
  }

  @PostMapping("/login")
  public AuthSessionResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpServletRequest) {
    return AuthSessionResponse.fromService(
        authWorkflowService.login(
            new AuthWorkflowService.LoginCommand(
                request.email(),
                request.password(),
                request.workspaceId()),
            clientContext(httpServletRequest)));
  }

  @PostMapping("/refresh")
  public AuthSessionResponse refresh(
      @Valid @RequestBody RefreshRequest request,
      HttpServletRequest httpServletRequest) {
    return AuthSessionResponse.fromService(
        authWorkflowService.refresh(
            new AuthWorkflowService.RefreshCommand(
                request.refreshToken(),
                request.workspaceId()),
            clientContext(httpServletRequest)));
  }

  @GetMapping("/me")
  public AuthSessionResponse me(@RequestHeader("Authorization") String authorizationHeader) {
    return AuthSessionResponse.fromService(
        authWorkflowService.me(extractBearerToken(authorizationHeader)));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestHeader("Authorization") String authorizationHeader) {
    authWorkflowService.logout(extractBearerToken(authorizationHeader));
  }

  @PostMapping("/verification/request")
  public ChallengeDispatchResponse requestEmailVerification(
      @Valid @RequestBody VerificationRequest request) {
    return ChallengeDispatchResponse.fromService(
        authWorkflowService.requestEmailVerification(request.email()));
  }

  @PostMapping("/verification/confirm")
  public ChallengeCompletionResponse confirmEmailVerification(
      @Valid @RequestBody TokenConfirmationRequest request) {
    return ChallengeCompletionResponse.fromService(
        authWorkflowService.confirmEmailVerification(request.token()));
  }

  @PostMapping("/password/request-reset")
  public ChallengeDispatchResponse requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request) {
    return ChallengeDispatchResponse.fromService(
        authWorkflowService.requestPasswordReset(request.email()));
  }

  @PostMapping("/password/reset")
  public ChallengeCompletionResponse resetPassword(
      @Valid @RequestBody PasswordResetConfirmationRequest request) {
    return ChallengeCompletionResponse.fromService(
        authWorkflowService.resetPassword(request.token(), request.newPassword()));
  }

  @PostMapping("/invites/accept")
  public AuthSessionResponse acceptInvite(
      @RequestHeader("Authorization") String authorizationHeader,
      @Valid @RequestBody InviteAcceptanceRequest request,
      HttpServletRequest httpServletRequest) {
    return AuthSessionResponse.fromService(
        authWorkflowService.acceptInvite(
            new AuthWorkflowService.InviteAcceptanceCommand(
                extractBearerToken(authorizationHeader),
                request.refreshToken(),
                request.inviteToken()),
            clientContext(httpServletRequest)));
  }

  @PostMapping("/workspaces/switch")
  public AuthSessionResponse switchWorkspace(
      @RequestHeader("Authorization") String authorizationHeader,
      @Valid @RequestBody SwitchWorkspaceRequest request,
      HttpServletRequest httpServletRequest) {
    return AuthSessionResponse.fromService(
        authWorkflowService.switchWorkspace(
            new AuthWorkflowService.SwitchWorkspaceCommand(
                extractBearerToken(authorizationHeader),
                request.refreshToken(),
                request.workspaceId()),
            clientContext(httpServletRequest)));
  }

  private AuthWorkflowService.ClientContext clientContext(HttpServletRequest request) {
    return new AuthWorkflowService.ClientContext(
        request.getRemoteAddr(),
        request.getHeader("User-Agent"));
  }

  private String extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Authorization header must use the Bearer scheme");
    }
    return authorizationHeader.substring("Bearer ".length()).trim();
  }

  public record SignUpRequest(
      @Email @NotBlank String email,
      @NotBlank String password,
      @NotBlank String displayName,
      String workspaceName,
      String locale,
      String timezone) {}

  public record LoginRequest(
      @Email @NotBlank String email,
      @NotBlank String password,
      UUID workspaceId) {}

  public record RefreshRequest(@NotBlank String refreshToken, UUID workspaceId) {}

  public record VerificationRequest(@Email @NotBlank String email) {}

  public record PasswordResetRequest(@Email @NotBlank String email) {}

  public record TokenConfirmationRequest(@NotBlank String token) {}

  public record PasswordResetConfirmationRequest(
      @NotBlank String token,
      @NotBlank String newPassword) {}

  public record InviteAcceptanceRequest(
      @NotBlank String inviteToken,
      @NotBlank String refreshToken) {}

  public record SwitchWorkspaceRequest(
      @NotNull UUID workspaceId,
      @NotBlank String refreshToken) {}

  public record AuthSessionResponse(
      UserResponse user,
      WorkspaceResponse currentWorkspace,
      List<WorkspaceResponse> workspaces,
      SessionTokensResponse session,
      DevelopmentTokensResponse developmentTokens) {
    static AuthSessionResponse fromService(AuthSessionView view) {
      return new AuthSessionResponse(
          view.user() == null ? null : UserResponse.fromService(view.user()),
          view.currentWorkspace() == null ? null : WorkspaceResponse.fromService(view.currentWorkspace()),
          view.workspaces() == null
              ? List.of()
              : view.workspaces().stream().map(WorkspaceResponse::fromService).toList(),
          view.accessToken() == null
              ? null
              : new SessionTokensResponse(
                  view.accessToken(),
                  view.refreshToken(),
                  view.accessTokenExpiresAt(),
                  view.refreshTokenExpiresAt()),
          new DevelopmentTokensResponse(view.emailVerificationToken(), view.passwordResetToken()));
    }
  }

  public record UserResponse(
      UUID userId,
      String email,
      String displayName,
      String accountStatus,
      String locale,
      String timezone,
      boolean emailVerified) {
    static UserResponse fromService(AuthWorkflowService.UserView userView) {
      return new UserResponse(
          userView.userId(),
          userView.email(),
          userView.displayName(),
          userView.accountStatus(),
          userView.locale(),
          userView.timezone(),
          userView.emailVerified());
    }
  }

  public record WorkspaceResponse(
      UUID membershipId,
      UUID workspaceId,
      String workspaceName,
      String workspaceSlug,
      String workspaceStatus,
      String roleCode,
      String roleName,
      String membershipStatus,
      java.time.Instant joinedAt,
      Set<String> permissions) {
    static WorkspaceResponse fromService(AuthWorkflowService.WorkspaceView workspaceView) {
      return new WorkspaceResponse(
          workspaceView.membershipId(),
          workspaceView.workspaceId(),
          workspaceView.workspaceName(),
          workspaceView.workspaceSlug(),
          workspaceView.workspaceStatus(),
          workspaceView.roleCode(),
          workspaceView.roleName(),
          workspaceView.membershipStatus(),
          workspaceView.joinedAt(),
          workspaceView.permissions());
    }
  }

  public record SessionTokensResponse(
      String accessToken,
      String refreshToken,
      java.time.Instant accessTokenExpiresAt,
      java.time.Instant refreshTokenExpiresAt) {}

  public record DevelopmentTokensResponse(
      String emailVerificationToken,
      String passwordResetToken) {}

  public record ChallengeDispatchResponse(
      String challengeType,
      boolean accepted,
      java.time.Instant expiresAt,
      String challengeToken) {
    static ChallengeDispatchResponse fromService(AuthWorkflowService.ChallengeDispatchView view) {
      return new ChallengeDispatchResponse(
          view.challengeType(),
          view.accepted(),
          view.expiresAt(),
          view.challengeToken());
    }
  }

  public record ChallengeCompletionResponse(
      String email,
      String challengeType,
      java.time.Instant completedAt) {
    static ChallengeCompletionResponse fromService(AuthWorkflowService.ChallengeCompletionView view) {
      return new ChallengeCompletionResponse(
          view.email(),
          view.challengeType(),
          view.completedAt());
    }
  }
}
