package com.odonta.polity.result;

import java.time.OffsetDateTime;

public record MembershipInvitationCompletionResult(
    MembershipInvitationCompletionStatus status,
    int attemptCount,
    String lastError,
    OffsetDateTime actionExpiresAt,
    OffsetDateTime completedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
