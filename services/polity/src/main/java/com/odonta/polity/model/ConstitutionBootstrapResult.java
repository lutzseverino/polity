package com.odonta.polity.model;

import java.time.OffsetDateTime;

public record ConstitutionBootstrapResult(
    boolean complete,
    OffsetDateTime completedAt,
    int minimumFullGovernmentMembers,
    long activeMemberCount,
    long standingMemberCount) {}
