package com.odonta.polity.result;

import com.odonta.polity.model.MembershipInvitationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipInvitationResult(
    UUID id,
    UUID polityId,
    String polityName,
    String email,
    String invitedByName,
    MembershipInvitationStatus status,
    OffsetDateTime invitedAt,
    OffsetDateTime respondedAt) {}
