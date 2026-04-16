package com.nexora.user.repository;

import com.nexora.user.domain.WorkspaceAccessSummary;
import com.nexora.user.domain.WorkspaceInviteRecord;
import com.nexora.user.domain.WorkspaceMemberSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserWorkspaceRepository {

  private final JdbcClient jdbcClient;

  public UserWorkspaceRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public boolean slugExists(String slug) {
    Integer count =
        jdbcClient.sql("SELECT COUNT(*) FROM workspaces WHERE slug = :slug")
            .param("slug", slug)
            .query(Integer.class)
            .single();
    return count != null && count > 0;
  }

  public void insertWorkspace(
      UUID workspaceId,
      String slug,
      String name,
      String billingEmail,
      String locale,
      String timezone) {
    jdbcClient.sql(
            """
            INSERT INTO workspaces (
              id, slug, name, status, billing_email, default_locale, default_timezone
            ) VALUES (
              :id, :slug, :name, 'active', :billingEmail, :locale, :timezone
            )
            """)
        .param("id", workspaceId)
        .param("slug", slug)
        .param("name", name)
        .param("billingEmail", billingEmail)
        .param("locale", locale)
        .param("timezone", timezone)
        .update();
  }

  public void insertRole(
      UUID roleId,
      UUID workspaceId,
      String code,
      String name,
      String description) {
    jdbcClient.sql(
            """
            INSERT INTO roles (id, workspace_id, scope, code, name, description, is_system)
            VALUES (:id, :workspaceId, 'workspace', :code, :name, :description, FALSE)
            """)
        .param("id", roleId)
        .param("workspaceId", workspaceId)
        .param("code", code)
        .param("name", name)
        .param("description", description)
        .update();
  }

  public void insertRolePermission(UUID roleId, String permissionKey) {
    jdbcClient.sql(
            "INSERT INTO role_permissions (role_id, permission_key) VALUES (:roleId, :permissionKey)")
        .param("roleId", roleId)
        .param("permissionKey", permissionKey)
        .update();
  }

  public void insertMembership(
      UUID membershipId,
      UUID workspaceId,
      UUID userId,
      UUID roleId,
      String status,
      Instant joinedAt) {
    jdbcClient.sql(
            """
            INSERT INTO workspace_memberships (
              id, workspace_id, user_id, role_id, status, joined_at
            ) VALUES (
              :id, :workspaceId, :userId, :roleId, :status, :joinedAt
            )
            """)
        .param("id", membershipId)
        .param("workspaceId", workspaceId)
        .param("userId", userId)
        .param("roleId", roleId)
        .param("status", status)
        .param("joinedAt", joinedAt == null ? null : Timestamp.from(joinedAt))
        .update();
  }

  public Optional<MembershipRecord> findMembership(UUID workspaceId, UUID userId) {
    return jdbcClient.sql(
            """
            SELECT id, role_id, status, joined_at
            FROM workspace_memberships
            WHERE workspace_id = :workspaceId AND user_id = :userId
            """)
        .param("workspaceId", workspaceId)
        .param("userId", userId)
        .query(this::mapMembershipRecord)
        .optional();
  }

  public void updateMembership(UUID membershipId, UUID roleId, String status, Instant joinedAt) {
    jdbcClient.sql(
            """
            UPDATE workspace_memberships
            SET role_id = :roleId, status = :status, joined_at = :joinedAt
            WHERE id = :id
            """)
        .param("id", membershipId)
        .param("roleId", roleId)
        .param("status", status)
        .param("joinedAt", joinedAt == null ? null : Timestamp.from(joinedAt))
        .update();
  }

  public void insertApprovalRoute(UUID routeId, UUID workspaceId, String name, String description) {
    jdbcClient.sql(
            """
            INSERT INTO approval_routes (
              id, workspace_id, name, description, min_approvers, is_default
            ) VALUES (
              :id, :workspaceId, :name, :description, 1, TRUE
            )
            """)
        .param("id", routeId)
        .param("workspaceId", workspaceId)
        .param("name", name)
        .param("description", description)
        .update();
  }

  public void insertApprovalRouteStep(
      UUID stepId,
      UUID approvalRouteId,
      UUID roleId) {
    jdbcClient.sql(
            """
            INSERT INTO approval_route_steps (
              id, approval_route_id, step_order, role_id, approver_user_id, required_approvals
            ) VALUES (
              :id, :approvalRouteId, 1, :roleId, NULL, 1
            )
            """)
        .param("id", stepId)
        .param("approvalRouteId", approvalRouteId)
        .param("roleId", roleId)
        .update();
  }

  public void insertAuditEntry(
      UUID auditEntryId,
      UUID workspaceId,
      UUID actorUserId,
      String actionType,
      String targetType,
      UUID targetId,
      String metadataJson) {
    jdbcClient.sql(
            """
            INSERT INTO audit_entries (
              id, workspace_id, actor_user_id, source_service, action_type, target_type, target_id, metadata
            ) VALUES (
              :id, :workspaceId, :actorUserId, 'user-service', :actionType, :targetType, :targetId, CAST(:metadata AS jsonb)
            )
            """)
        .param("id", auditEntryId)
        .param("workspaceId", workspaceId)
        .param("actorUserId", actorUserId)
        .param("actionType", actionType)
        .param("targetType", targetType)
        .param("targetId", targetId)
        .param("metadata", metadataJson)
        .update();
  }

  public Optional<RoleRecord> findWorkspaceRoleByCode(UUID workspaceId, String roleCode) {
    return jdbcClient.sql(
            """
            SELECT id, code, name
            FROM roles
            WHERE workspace_id = :workspaceId AND code = :roleCode
            """)
        .param("workspaceId", workspaceId)
        .param("roleCode", roleCode)
        .query((resultSet, rowNum) ->
            new RoleRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("code"),
                resultSet.getString("name")))
        .optional();
  }

  public List<WorkspaceAccessSummary> findWorkspaceAccess(UUID userId) {
    List<WorkspaceAccessFlatRow> rows =
        jdbcClient.sql(
                """
                SELECT
                  wm.id AS membership_id,
                  wm.workspace_id,
                  wm.status AS membership_status,
                  wm.joined_at,
                  w.name AS workspace_name,
                  w.slug AS workspace_slug,
                  w.status AS workspace_status,
                  r.code AS role_code,
                  r.name AS role_name,
                  rp.permission_key
                FROM workspace_memberships wm
                JOIN workspaces w ON w.id = wm.workspace_id
                JOIN roles r ON r.id = wm.role_id
                LEFT JOIN role_permissions rp ON rp.role_id = r.id
                WHERE wm.user_id = :userId AND wm.status = 'active'
                ORDER BY w.created_at ASC, rp.permission_key ASC
                """)
            .param("userId", userId)
            .query(this::mapWorkspaceAccessFlatRow)
            .list();

    Map<UUID, WorkspaceAccessAccumulator> accumulators = new LinkedHashMap<>();
    for (WorkspaceAccessFlatRow row : rows) {
      WorkspaceAccessAccumulator accumulator =
          accumulators.computeIfAbsent(row.membershipId(), ignored -> new WorkspaceAccessAccumulator(row));
      if (row.permissionKey() != null && !row.permissionKey().isBlank()) {
        accumulator.permissions().add(row.permissionKey());
      }
    }

    return accumulators.values().stream()
        .map(WorkspaceAccessAccumulator::toSummary)
        .toList();
  }

  public List<WorkspaceMemberSummary> findWorkspaceMembers(UUID workspaceId) {
    return jdbcClient.sql(
            """
            SELECT
              wm.id AS membership_id,
              wm.workspace_id,
              wm.user_id,
              wm.status,
              wm.joined_at,
              r.code AS role_code,
              r.name AS role_name
            FROM workspace_memberships wm
            JOIN roles r ON r.id = wm.role_id
            WHERE wm.workspace_id = :workspaceId
            ORDER BY wm.created_at ASC
            """)
        .param("workspaceId", workspaceId)
        .query(
            (resultSet, rowNum) ->
                new WorkspaceMemberSummary(
                    resultSet.getObject("membership_id", UUID.class),
                    resultSet.getObject("workspace_id", UUID.class),
                    resultSet.getObject("user_id", UUID.class),
                    resultSet.getString("role_code"),
                    resultSet.getString("role_name"),
                    resultSet.getString("status"),
                    toInstant(resultSet.getTimestamp("joined_at"))))
        .list();
  }

  public void insertInvite(
      UUID inviteId,
      UUID workspaceId,
      String email,
      UUID roleId,
      UUID invitedByUserId,
      String tokenHash,
      Instant expiresAt) {
    jdbcClient.sql(
            """
            INSERT INTO workspace_invites (
              id, workspace_id, email, role_id, invited_by_user_id, token_hash, status, expires_at
            ) VALUES (
              :id, :workspaceId, :email, :roleId, :invitedByUserId, :tokenHash, 'pending', :expiresAt
            )
            """)
        .param("id", inviteId)
        .param("workspaceId", workspaceId)
        .param("email", email)
        .param("roleId", roleId)
        .param("invitedByUserId", invitedByUserId)
        .param("tokenHash", tokenHash)
        .param("expiresAt", Timestamp.from(expiresAt))
        .update();
  }

  public Optional<WorkspaceInviteRecord> findInviteByTokenHash(String tokenHash) {
    return jdbcClient.sql(
            """
            SELECT
              wi.id AS invite_id,
              wi.workspace_id,
              wi.email,
              wi.role_id,
              wi.invited_by_user_id,
              wi.status,
              wi.expires_at,
              w.name AS workspace_name,
              r.code AS role_code
            FROM workspace_invites wi
            JOIN workspaces w ON w.id = wi.workspace_id
            JOIN roles r ON r.id = wi.role_id
            WHERE wi.token_hash = :tokenHash
            """)
        .param("tokenHash", tokenHash)
        .query(
            (resultSet, rowNum) ->
                new WorkspaceInviteRecord(
                    resultSet.getObject("invite_id", UUID.class),
                    resultSet.getObject("workspace_id", UUID.class),
                    resultSet.getString("workspace_name"),
                    resultSet.getString("email"),
                    resultSet.getObject("role_id", UUID.class),
                    resultSet.getString("role_code"),
                    resultSet.getObject("invited_by_user_id", UUID.class),
                    resultSet.getString("status"),
                    toInstant(resultSet.getTimestamp("expires_at"))))
        .optional();
  }

  public void markInviteAccepted(UUID inviteId, UUID acceptedByUserId, Instant acceptedAt) {
    jdbcClient.sql(
            """
            UPDATE workspace_invites
            SET status = 'accepted', accepted_at = :acceptedAt, accepted_by_user_id = :acceptedByUserId
            WHERE id = :inviteId
            """)
        .param("inviteId", inviteId)
        .param("acceptedAt", Timestamp.from(acceptedAt))
        .param("acceptedByUserId", acceptedByUserId)
        .update();
  }

  private MembershipRecord mapMembershipRecord(ResultSet resultSet, int rowNumber) throws SQLException {
    return new MembershipRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("role_id", UUID.class),
        resultSet.getString("status"),
        toInstant(resultSet.getTimestamp("joined_at")));
  }

  private WorkspaceAccessFlatRow mapWorkspaceAccessFlatRow(ResultSet resultSet, int rowNumber)
      throws SQLException {
    return new WorkspaceAccessFlatRow(
        resultSet.getObject("membership_id", UUID.class),
        resultSet.getObject("workspace_id", UUID.class),
        resultSet.getString("workspace_name"),
        resultSet.getString("workspace_slug"),
        resultSet.getString("workspace_status"),
        resultSet.getString("role_code"),
        resultSet.getString("role_name"),
        resultSet.getString("membership_status"),
        toInstant(resultSet.getTimestamp("joined_at")),
        resultSet.getString("permission_key"));
  }

  private Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  public record RoleRecord(UUID id, String code, String name) {}

  public record MembershipRecord(UUID id, UUID roleId, String status, Instant joinedAt) {}

  private record WorkspaceAccessFlatRow(
      UUID membershipId,
      UUID workspaceId,
      String workspaceName,
      String workspaceSlug,
      String workspaceStatus,
      String roleCode,
      String roleName,
      String membershipStatus,
      Instant joinedAt,
      String permissionKey) {}

  private static final class WorkspaceAccessAccumulator {
    private final WorkspaceAccessFlatRow row;
    private final Set<String> permissions = new LinkedHashSet<>();

    private WorkspaceAccessAccumulator(WorkspaceAccessFlatRow row) {
      this.row = row;
    }

    private Set<String> permissions() {
      return permissions;
    }

    private WorkspaceAccessSummary toSummary() {
      return new WorkspaceAccessSummary(
          row.membershipId(),
          row.workspaceId(),
          row.workspaceName(),
          row.workspaceSlug(),
          row.workspaceStatus(),
          row.roleCode(),
          row.roleName(),
          row.membershipStatus(),
          row.joinedAt(),
          Set.copyOf(new ArrayList<>(permissions)));
    }
  }
}
