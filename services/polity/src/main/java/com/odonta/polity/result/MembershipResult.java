package com.odonta.polity.result;

import com.odonta.polity.model.MembershipStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipResult(
    UUID id,
    UUID userId,
    String name,
    String email,
    MembershipStatus status,
    OffsetDateTime admittedAt) {}
