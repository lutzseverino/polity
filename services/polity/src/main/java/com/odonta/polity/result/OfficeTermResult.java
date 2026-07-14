package com.odonta.polity.result;

import com.odonta.polity.model.OfficeTermStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OfficeTermResult(
    UUID id,
    UUID officeId,
    String officeName,
    String officeNameKey,
    UUID membershipId,
    String memberName,
    OfficeTermStatus status,
    OffsetDateTime startedAt,
    OffsetDateTime endsAt) {}
