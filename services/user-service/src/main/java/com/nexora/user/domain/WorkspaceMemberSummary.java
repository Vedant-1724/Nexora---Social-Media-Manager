package com.nexora.user.domain;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberSummary(
    UUID membershipId,
    UUID workspaceId,
    UUID userId,
    String roleCode,
    String roleName,
    String status,
    Instant joinedAt) {}
