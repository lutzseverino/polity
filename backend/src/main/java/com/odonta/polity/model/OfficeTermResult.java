package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfficeTermResult(
    UUID id,
    UUID officeId,
    String officeName,
    UUID membershipId,
    String memberName,
    OfficeTermStatus status,
    OffsetDateTime startedAt,
    OffsetDateTime endsAt) {}
