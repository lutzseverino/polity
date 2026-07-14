package com.odonta.polity.result;

import java.time.OffsetDateTime;

public record GovernmentFormationResult(
    boolean complete,
    OffsetDateTime completedAt,
    int minimumFullGovernmentMembers,
    long activeMemberCount,
    long standingMemberCount) {}
