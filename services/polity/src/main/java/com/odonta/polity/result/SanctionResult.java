package com.odonta.polity.result;

import com.odonta.polity.model.SanctionStatus;
import com.odonta.polity.model.SanctionType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SanctionResult(
    UUID id,
    UUID targetMembershipId,
    String targetName,
    SanctionType type,
    SanctionStatus status,
    String reason,
    OffsetDateTime startedAt,
    OffsetDateTime endsAt) {}
