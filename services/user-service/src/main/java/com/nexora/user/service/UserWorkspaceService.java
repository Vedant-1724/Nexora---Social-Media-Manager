package com.nexora.user.service;

import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.security.TokenHashingUtils;
import com.nexora.user.domain.WorkspaceAccessSummary;
import com.nexora.user.domain.WorkspaceInviteRecord;
import com.nexora.user.domain.WorkspaceMemberSummary;
import com.nexora.user.domain.WorkspaceRoleCatalog;
import com.nexora.user.repository.UserWorkspaceRepository;
import com.nexora.user.repository.UserWorkspaceRepository.MembershipRecord;
import com.nexora.user.repository.UserWorkspaceRepository.RoleRecord;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWorkspaceService {

  private final UserWorkspaceRepository workspaceRepository;
  private final Clock clock;

  public UserWorkspaceService(UserWorkspaceRepository workspaceRepository, Clock clock) {
    this.workspaceRepository = workspaceRepository;
    this.clock = clock;
  }

  @Transactional
  public WorkspaceAccessSummary bootstrapWorkspace(
      UUID userId,
      String email,
      String workspaceName,
      String locale,
      String timezone) {
    UUID workspaceId = UUID.randomUUID();
    String slug = ensureUniqueSlug(workspaceName);
    workspaceRepository.insertWorkspace(
        workspaceId,
        slug,
        workspaceName,
        email,
        locale == null || locale.isBlank() ? "en" : locale,
        timezone == null || timezone.isBlank() ? "Asia/Calcutta" : timezone);

    Map<String, UUID> roleIds = new java.util.LinkedHashMap<>();
    for (WorkspaceRoleCatalog.RoleTemplate template : WorkspaceRoleCatalog.defaultWorkspaceRoles()) {
      UUID roleId = UUID.randomUUID();
      roleIds.put(template.code(), roleId);
      workspaceRepository.insertRole(roleId, workspaceId, template.code(), template.name(), template.description());
      template.permissions().forEach(permission -> workspaceRepository.insertRolePermission(roleId, permission));
    }

    workspaceRepository.insertMembership(
        UUID.randomUUID(),
        workspaceId,
        userId,
        roleIds.get("owner"),
        "active",
        Instant.now(clock));

    UUID approvalRouteId = UUID.randomUUID();
    workspaceRepository.insertApprovalRoute(
        approvalRouteId,
        workspaceId,
        "Default Content Approval",
        "Owner approval route created during workspace bootstrap.");
    workspaceRepository.insertApprovalRouteStep(UUID.randomUUID(), approvalRouteId, roleIds.get("owner"));

    workspaceRepository.insertAuditEntry(
        UUID.randomUUID(),
        workspaceId,
        userId,
        "workspace.bootstrap.completed",
        "workspaces",
        workspaceId,
        "{\"workspaceName\":\"" + escapeJson(workspaceName) + "\"}");

    return findWorkspaceAccess(userId, workspaceId)
        .orElseThrow(() -> new IllegalStateException("Workspace bootstrap did not create an owner membership"));
  }

  @Transactional(readOnly = true)
  public List<WorkspaceAccessSummary> listWorkspaceAccess(UUID userId) {
    return workspaceRepository.findWorkspaceAccess(userId);
  }

  @Transactional(readOnly = true)
  public Optional<WorkspaceAccessSummary> findWorkspaceAccess(UUID userId, UUID workspaceId) {
    return listWorkspaceAccess(userId).stream()
        .filter(access -> access.workspaceId().equals(workspaceId))
        .findFirst();
  }

  @Transactional
  public InviteCreationResult createInvite(
      UUID actorUserId,
      UUID workspaceId,
      String email,
      String roleCode) {
    WorkspaceAccessSummary actorAccess = requireWorkspaceAccess(actorUserId, workspaceId);
    if (!actorAccess.permissions().contains("workspace.members.invite")) {
      throw new ForbiddenException("Actor does not have invite permission for this workspace");
    }

    RoleRecord role =
        workspaceRepository.findWorkspaceRoleByCode(workspaceId, roleCode)
            .orElseThrow(() -> new IllegalArgumentException("Unknown workspace role: " + roleCode));

    String rawInviteToken = generateOpaqueToken();
    Instant expiresAt = Instant.now(clock).plus(7, ChronoUnit.DAYS);
    UUID inviteId = UUID.randomUUID();

    workspaceRepository.insertInvite(
        inviteId,
        workspaceId,
        email.toLowerCase(Locale.ROOT),
        role.id(),
        actorUserId,
        TokenHashingUtils.sha256(rawInviteToken),
        expiresAt);

    workspaceRepository.insertAuditEntry(
        UUID.randomUUID(),
        workspaceId,
        actorUserId,
        "workspace.member.invited",
        "workspace_invites",
        inviteId,
        "{\"email\":\"" + escapeJson(email.toLowerCase(Locale.ROOT)) + "\",\"roleCode\":\"" + role.code() + "\"}");

    return new InviteCreationResult(inviteId, role.code(), expiresAt, rawInviteToken);
  }

  @Transactional
  public WorkspaceAccessSummary acceptInvite(String rawInviteToken, UUID userId, String email) {
    WorkspaceInviteRecord invite =
        workspaceRepository.findInviteByTokenHash(TokenHashingUtils.sha256(rawInviteToken))
            .orElseThrow(() -> new IllegalArgumentException("Invite token is invalid"));

    if (!"pending".equals(invite.status())) {
      throw new IllegalArgumentException("Invite is no longer pending");
    }
    if (invite.expiresAt().isBefore(Instant.now(clock))) {
      throw new IllegalArgumentException("Invite has expired");
    }
    if (!invite.email().equalsIgnoreCase(email)) {
      throw new IllegalArgumentException("Invite email does not match the accepting account");
    }

    Instant acceptedAt = Instant.now(clock);
    Optional<MembershipRecord> existingMembership =
        workspaceRepository.findMembership(invite.workspaceId(), userId);
    if (existingMembership.isPresent()) {
      workspaceRepository.updateMembership(
          existingMembership.get().id(),
          invite.roleId(),
          "active",
          existingMembership.get().joinedAt() == null ? acceptedAt : existingMembership.get().joinedAt());
    } else {
      workspaceRepository.insertMembership(
          UUID.randomUUID(),
          invite.workspaceId(),
          userId,
          invite.roleId(),
          "active",
          acceptedAt);
    }

    workspaceRepository.markInviteAccepted(invite.inviteId(), userId, acceptedAt);
    workspaceRepository.insertAuditEntry(
        UUID.randomUUID(),
        invite.workspaceId(),
        userId,
        "workspace.member.accepted",
        "workspace_invites",
        invite.inviteId(),
        "{\"email\":\"" + escapeJson(email.toLowerCase(Locale.ROOT)) + "\"}");

    return findWorkspaceAccess(userId, invite.workspaceId())
        .orElseThrow(() -> new IllegalStateException("Invite acceptance did not result in workspace access"));
  }

  @Transactional(readOnly = true)
  public List<WorkspaceMemberSummary> listMembers(UUID actorUserId, UUID workspaceId) {
    WorkspaceAccessSummary actorAccess = requireWorkspaceAccess(actorUserId, workspaceId);
    if (!actorAccess.permissions().contains("workspace.members.read")) {
      throw new ForbiddenException("Actor does not have member read access for this workspace");
    }

    return workspaceRepository.findWorkspaceMembers(workspaceId);
  }

  private WorkspaceAccessSummary requireWorkspaceAccess(UUID userId, UUID workspaceId) {
    return findWorkspaceAccess(userId, workspaceId)
        .orElseThrow(() -> new ForbiddenException("User does not belong to the requested workspace"));
  }

  private String ensureUniqueSlug(String workspaceName) {
    String baseSlug = slugify(workspaceName);
    String candidate = baseSlug;
    int suffix = 2;
    while (workspaceRepository.slugExists(candidate)) {
      candidate = baseSlug + "-" + suffix++;
    }
    return candidate;
  }

  private String slugify(String input) {
    String normalized =
        Normalizer.normalize(Objects.requireNonNullElse(input, "workspace"), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
    String slug = normalized.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
    return slug.isBlank() ? "workspace" : slug;
  }

  private String generateOpaqueToken() {
    byte[] bytes = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  public record InviteCreationResult(
      UUID inviteId,
      String roleCode,
      Instant expiresAt,
      String inviteToken) {}
}
