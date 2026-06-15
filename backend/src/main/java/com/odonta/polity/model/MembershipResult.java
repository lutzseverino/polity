package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipResult(
    UUID id,
    UUID userId,
    String name,
    String email,
    MembershipStatus status,
    OffsetDateTime admittedAt) {}
