package com.nexora.user.domain;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceInviteRecord(
    UUID inviteId,
    UUID workspaceId,
    String workspaceName,
    String email,
    UUID roleId,
    String roleCode,
    UUID invitedByUserId,
    String status,
    Instant expiresAt) {}
