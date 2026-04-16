package com.nexora.user.domain;

import java.util.List;

public final class WorkspaceRoleCatalog {

  private WorkspaceRoleCatalog() {}

  public static List<RoleTemplate> defaultWorkspaceRoles() {
    return List.of(
        new RoleTemplate(
            "owner",
            "Owner",
            "Full administrative ownership for the workspace.",
            List.of(
                "workspace.manage",
                "workspace.read",
                "workspace.switch",
                "workspace.members.read",
                "workspace.members.invite",
                "workspace.members.manage",
                "posts.create",
                "posts.approve")),
        new RoleTemplate(
            "admin",
            "Admin",
            "Operational administrator for approvals and team workflows.",
            List.of(
                "workspace.read",
                "workspace.switch",
                "workspace.members.read",
                "workspace.members.invite",
                "posts.create",
                "posts.approve")),
        new RoleTemplate(
            "editor",
            "Editor",
            "Creates and schedules content inside the workspace.",
            List.of(
                "workspace.read",
                "workspace.switch",
                "workspace.members.read",
                "posts.create")),
        new RoleTemplate(
            "viewer",
            "Viewer",
            "Read-only access for reporting and workspace visibility.",
            List.of(
                "workspace.read",
                "workspace.switch")));
  }

  public record RoleTemplate(
      String code,
      String name,
      String description,
      List<String> permissions) {}
}
