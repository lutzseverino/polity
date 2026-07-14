package com.odonta.polity.result;

import com.odonta.polity.model.AppealStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AppealResult(
    UUID id,
    UUID sanctionId,
    UUID appellantMembershipId,
    String appellantName,
    AppealStatus status,
    String reason,
    OffsetDateTime decidedAt) {}
