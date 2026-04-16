package com.nexora.user.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WorkspaceAccessSummary(
    UUID membershipId,
    UUID workspaceId,
    String workspaceName,
    String workspaceSlug,
    String workspaceStatus,
    String roleCode,
    String roleName,
    String membershipStatus,
    Instant joinedAt,
    Set<String> permissions) {}
