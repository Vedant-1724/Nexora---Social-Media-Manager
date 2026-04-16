package com.nexora.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.security.TokenHashingUtils;
import com.nexora.user.domain.WorkspaceAccessSummary;
import com.nexora.user.domain.WorkspaceInviteRecord;
import com.nexora.user.repository.UserWorkspaceRepository;
import com.nexora.user.service.UserWorkspaceService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserWorkspaceServiceTests {

  @Mock
  private UserWorkspaceRepository userWorkspaceRepository;

  private UserWorkspaceService userWorkspaceService;
  private Clock clock;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-03-31T12:00:00Z"), ZoneOffset.UTC);
    userWorkspaceService = new UserWorkspaceService(userWorkspaceRepository, clock);
  }

  @Test
  void bootstrapWorkspaceCreatesDefaultRolesApprovalAndAudit() {
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    AtomicReference<UUID> createdWorkspaceId = new AtomicReference<>();
    AtomicReference<String> createdWorkspaceName = new AtomicReference<>();

    when(userWorkspaceRepository.slugExists(anyString())).thenReturn(false);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              createdWorkspaceId.set(invocation.getArgument(0));
              createdWorkspaceName.set(invocation.getArgument(2));
              return null;
            })
        .when(userWorkspaceRepository)
        .insertWorkspace(any(), anyString(), anyString(), anyString(), anyString(), anyString());
    when(userWorkspaceRepository.findWorkspaceAccess(userId))
        .thenAnswer(
            invocation ->
                List.of(
                    new WorkspaceAccessSummary(
                        UUID.fromString("30000000-0000-0000-0000-000000000001"),
                        createdWorkspaceId.get(),
                        createdWorkspaceName.get(),
                        "northstar-creative",
                        "active",
                        "owner",
                        "Owner",
                        "active",
                        Instant.now(clock),
                        Set.of("workspace.members.invite", "workspace.members.read"))));

    WorkspaceAccessSummary result =
        userWorkspaceService.bootstrapWorkspace(
            userId,
            "admin@nexora.dev",
            "Northstar Creative",
            "en",
            "Asia/Calcutta");

    assertThat(result.workspaceName()).isEqualTo("Northstar Creative");
    verify(userWorkspaceRepository, times(4)).insertRole(any(), any(), anyString(), anyString(), anyString());
    verify(userWorkspaceRepository).insertMembership(any(), any(), any(), any(), anyString(), any());
    verify(userWorkspaceRepository).insertApprovalRoute(any(), any(), anyString(), anyString());
    verify(userWorkspaceRepository).insertApprovalRouteStep(any(), any(), any());
    verify(userWorkspaceRepository).insertAuditEntry(any(), any(), any(), anyString(), anyString(), any(), anyString());
  }

  @Test
  void listMembersRejectsActorWithoutPermission() {
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    UUID workspaceId = UUID.fromString("10000000-0000-0000-0000-000000000001");

    when(userWorkspaceRepository.findWorkspaceAccess(userId))
        .thenReturn(
            List.of(
                new WorkspaceAccessSummary(
                    UUID.randomUUID(),
                    workspaceId,
                    "Northstar Creative",
                    "northstar-creative",
                    "active",
                    "editor",
                    "Editor",
                    "active",
                    Instant.now(clock),
                    Set.of("posts.create"))));

    assertThatThrownBy(() -> userWorkspaceService.listMembers(userId, workspaceId))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("member read");
  }

  @Test
  void acceptInviteActivatesWorkspaceMembership() {
    UUID workspaceId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    UUID roleId = UUID.fromString("10000000-0000-0000-0000-000000000011");
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    String rawToken = "invite-token";

    when(userWorkspaceRepository.findInviteByTokenHash(TokenHashingUtils.sha256(rawToken)))
        .thenReturn(
            Optional.of(
                new WorkspaceInviteRecord(
                    UUID.fromString("40000000-0000-0000-0000-000000000001"),
                    workspaceId,
                    "Northstar Creative",
                    "admin@nexora.dev",
                    roleId,
                    "owner",
                    UUID.fromString("50000000-0000-0000-0000-000000000001"),
                    "pending",
                    Instant.now(clock).plusSeconds(300))));
    when(userWorkspaceRepository.findMembership(workspaceId, userId)).thenReturn(Optional.empty());
    when(userWorkspaceRepository.findWorkspaceAccess(userId))
        .thenReturn(
            List.of(
                new WorkspaceAccessSummary(
                    UUID.randomUUID(),
                    workspaceId,
                    "Northstar Creative",
                    "northstar-creative",
                    "active",
                    "owner",
                    "Owner",
                    "active",
                    Instant.now(clock),
                    Set.of("workspace.members.invite", "workspace.members.read"))));

    WorkspaceAccessSummary accessSummary =
        userWorkspaceService.acceptInvite(rawToken, userId, "admin@nexora.dev");

    assertThat(accessSummary.roleCode()).isEqualTo("owner");
    verify(userWorkspaceRepository).insertMembership(any(), any(), any(), any(), anyString(), any());
    verify(userWorkspaceRepository).markInviteAccepted(any(), any(), any());
  }
}
