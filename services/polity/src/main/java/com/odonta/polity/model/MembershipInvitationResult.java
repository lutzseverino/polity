package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipInvitationResult(
    UUID id,
    UUID polityId,
    String polityName,
    String email,
    String invitedByName,
    InvitationStatus status,
    OffsetDateTime invitedAt,
    OffsetDateTime respondedAt) {}
