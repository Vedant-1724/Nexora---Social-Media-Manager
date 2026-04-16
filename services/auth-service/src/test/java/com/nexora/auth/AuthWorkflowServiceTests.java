package com.nexora.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.auth.config.AuthSecurityProperties;
import com.nexora.auth.repository.AuthRepository;
import com.nexora.auth.repository.AuthRepository.AuthMethodRecord;
import com.nexora.auth.repository.AuthRepository.RefreshSessionRecord;
import com.nexora.auth.repository.AuthRepository.UserIdentityRecord;
import com.nexora.auth.service.AuthSessionRevocationStore;
import com.nexora.auth.service.AuthUserAccessClient;
import com.nexora.auth.service.AuthWorkflowService;
import com.nexora.auth.service.JwtAccessTokenService;
import com.nexora.platform.core.auth.UnauthorizedException;
import com.nexora.platform.core.security.TokenHashingUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthWorkflowServiceTests {

  @Mock
  private AuthRepository authRepository;

  @Mock
  private AuthUserAccessClient authUserAccessClient;

  @Mock
  private AuthSessionRevocationStore authSessionRevocationStore;

  @Mock
  private PasswordEncoder passwordEncoder;

  private Clock clock;
  private JwtAccessTokenService jwtAccessTokenService;
  private AuthWorkflowService authWorkflowService;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);

    AuthSecurityProperties authSecurityProperties = new AuthSecurityProperties();
    authSecurityProperties.getJwt().setSecret("nexora-dev-secret-change-me-please-123456789");
    authSecurityProperties.setExposeDevelopmentTokens(true);

    jwtAccessTokenService = new JwtAccessTokenService(authSecurityProperties, clock);
    authWorkflowService =
        new AuthWorkflowService(
            authRepository,
            authUserAccessClient,
            authSessionRevocationStore,
            jwtAccessTokenService,
            passwordEncoder,
            authSecurityProperties,
            clock);
  }

  @Test
  void signUpCreatesSessionAndVerificationChallenge() {
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    UserIdentityRecord user =
        new UserIdentityRecord(userId, "admin@nexora.dev", "Nexora Admin", "pending", "en", "Asia/Calcutta");
    AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess = workspaceAccess(
        "Northstar Creative",
        "workspace.owner");

    when(authRepository.findUserByEmail("admin@nexora.dev")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("Nexora123!")).thenReturn("encoded-password");
    when(authUserAccessClient.bootstrapWorkspace(any(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(workspaceAccess);
    when(authRepository.findUserById(any())).thenReturn(Optional.of(user));
    when(authUserAccessClient.listWorkspaceAccess(userId)).thenReturn(List.of(workspaceAccess));

    AuthWorkflowService.AuthSessionView sessionView =
        authWorkflowService.signUp(
            new AuthWorkflowService.SignUpCommand(
                "admin@nexora.dev",
                "Nexora123!",
                "Nexora Admin",
                "Northstar Creative",
                "en",
                "Asia/Calcutta"),
            new AuthWorkflowService.ClientContext("127.0.0.1", "JUnit"));

    assertThat(sessionView.user().email()).isEqualTo("admin@nexora.dev");
    assertThat(sessionView.currentWorkspace().workspaceName()).isEqualTo("Northstar Creative");
    assertThat(sessionView.accessToken()).isNotBlank();
    assertThat(sessionView.refreshToken()).isNotBlank();
    assertThat(sessionView.emailVerificationToken()).isNotBlank();

    verify(authRepository).insertUserIdentity(any(), anyString(), anyString(), anyString(), anyString(), anyString());
    verify(authRepository).insertPasswordAuthMethod(any(), any(), anyString(), anyString(), anyBoolean());
    verify(authRepository).insertRefreshSession(any(), any(), anyString(), any(), anyString(), any(), any(), anyString(), anyString());
    verify(authRepository).insertVerificationChallenge(any(), any(), anyString(), anyString(), any());
  }

  @Test
  void loginCreatesRefreshSessionForVerifiedUser() {
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    UserIdentityRecord user =
        new UserIdentityRecord(userId, "admin@nexora.dev", "Nexora Admin", "active", "en", "Asia/Calcutta");
    AuthMethodRecord passwordMethod =
        new AuthMethodRecord(
            UUID.fromString("30000000-0000-0000-0000-000000000001"),
            userId,
            "password",
            "admin@nexora.dev",
            "encoded-password",
            true,
            Instant.now(clock));
    AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess = workspaceAccess(
        "Northstar Creative",
        "workspace.members.read");

    when(authRepository.findPrimaryPasswordMethodByEmail("admin@nexora.dev"))
        .thenReturn(Optional.of(passwordMethod));
    when(passwordEncoder.matches("Nexora123!", "encoded-password")).thenReturn(true);
    when(authRepository.findUserById(userId)).thenReturn(Optional.of(user));
    when(authUserAccessClient.listWorkspaceAccess(userId)).thenReturn(List.of(workspaceAccess));

    AuthWorkflowService.AuthSessionView sessionView =
        authWorkflowService.login(
            new AuthWorkflowService.LoginCommand("admin@nexora.dev", "Nexora123!", workspaceAccess.workspaceId()),
            new AuthWorkflowService.ClientContext("127.0.0.1", "JUnit"));

    assertThat(sessionView.user().email()).isEqualTo("admin@nexora.dev");
    assertThat(sessionView.currentWorkspace().workspaceId()).isEqualTo(workspaceAccess.workspaceId());
    assertThat(sessionView.accessToken()).isNotBlank();
    assertThat(sessionView.refreshToken()).isNotBlank();

    verify(authRepository)
        .insertRefreshSession(any(), any(), anyString(), any(), anyString(), any(), any(), anyString(), anyString());
  }

  @Test
  void refreshRejectsRevokedRefreshSession() {
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    UUID sessionId = UUID.fromString("20000000-0000-0000-0000-000000000001");
    String rawRefreshToken = "refresh-token";

    when(authRepository.findRefreshSessionByTokenHash(TokenHashingUtils.sha256(rawRefreshToken)))
        .thenReturn(
            Optional.of(
                new RefreshSessionRecord(
                    sessionId,
                    userId,
                    "refresh-hash",
                    UUID.fromString("10000000-0000-0000-0000-000000000001"),
                    "active",
                    Instant.now(clock),
                    Instant.now(clock).plusSeconds(600),
                    null,
                    "127.0.0.1",
                    "JUnit")));
    when(authSessionRevocationStore.isRefreshSessionRevoked(sessionId)).thenReturn(true);

    assertThatThrownBy(
            () ->
                authWorkflowService.refresh(
                    new AuthWorkflowService.RefreshCommand(rawRefreshToken, null),
                    new AuthWorkflowService.ClientContext("127.0.0.1", "JUnit")))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("revoked");
  }

  @Test
  void switchWorkspaceRotatesRefreshSessionAndRevokesOldAccessToken() {
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    UUID sessionId = UUID.fromString("20000000-0000-0000-0000-000000000001");
    UUID currentWorkspaceId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID nextWorkspaceId = UUID.fromString("10000000-0000-0000-0000-000000000002");
    String refreshToken = "phase-4-refresh";
    String accessToken =
        jwtAccessTokenService
            .issueToken(
                new JwtAccessTokenService.AccessTokenIdentity(
                    userId,
                    "admin@nexora.dev",
                    "Nexora Admin",
                    true,
                    currentWorkspaceId,
                    "Northstar Creative",
                    sessionId,
                    List.of("workspace.members.read")))
            .token();

    UserIdentityRecord user =
        new UserIdentityRecord(userId, "admin@nexora.dev", "Nexora Admin", "active", "en", "Asia/Calcutta");
    RefreshSessionRecord refreshSession =
        new RefreshSessionRecord(
            sessionId,
            userId,
            TokenHashingUtils.sha256(refreshToken),
            currentWorkspaceId,
            "active",
            Instant.now(clock),
            Instant.now(clock).plusSeconds(900),
            null,
            "127.0.0.1",
            "JUnit");

    AuthUserAccessClient.WorkspaceAccessResponse currentWorkspace = workspaceAccess(
        currentWorkspaceId,
        "Northstar Creative",
        "workspace.members.read");
    AuthUserAccessClient.WorkspaceAccessResponse nextWorkspace = workspaceAccess(
        nextWorkspaceId,
        "Launch Labs",
        "workspace.members.read");

    when(authSessionRevocationStore.isAccessTokenRevoked(anyString())).thenReturn(false);
    when(authRepository.findRefreshSessionByTokenHash(TokenHashingUtils.sha256(refreshToken)))
        .thenReturn(Optional.of(refreshSession));
    when(authSessionRevocationStore.isRefreshSessionRevoked(sessionId)).thenReturn(false);
    when(authRepository.findUserById(userId)).thenReturn(Optional.of(user));
    when(authUserAccessClient.listWorkspaceAccess(userId)).thenReturn(List.of(currentWorkspace, nextWorkspace));

    AuthWorkflowService.AuthSessionView sessionView =
        authWorkflowService.switchWorkspace(
            new AuthWorkflowService.SwitchWorkspaceCommand(accessToken, refreshToken, nextWorkspaceId),
            new AuthWorkflowService.ClientContext("127.0.0.1", "JUnit"));

    assertThat(sessionView.currentWorkspace().workspaceId()).isEqualTo(nextWorkspaceId);
    assertThat(sessionView.refreshToken()).isNotBlank();
    verify(authRepository).rotateRefreshSession(any(), anyString(), any(), any(), any(), anyString());
    verify(authSessionRevocationStore).revokeAccessToken(anyString(), any());
  }

  private AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess(String workspaceName, String permission) {
    return workspaceAccess(
        UUID.fromString("10000000-0000-0000-0000-000000000001"),
        workspaceName,
        permission);
  }

  private AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess(
      UUID workspaceId,
      String workspaceName,
      String permission) {
    return new AuthUserAccessClient.WorkspaceAccessResponse(
        UUID.randomUUID(),
        workspaceId,
        workspaceName,
        workspaceName.toLowerCase().replace(" ", "-"),
        "active",
        "owner",
        "Owner",
        "active",
        Instant.now(clock),
        Set.of(permission));
  }
}
