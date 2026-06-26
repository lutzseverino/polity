package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfficeTermReviewResult(
    UUID id,
    UUID officeTermId,
    UUID petitionerMembershipId,
    String petitionerName,
    UUID vacatedMembershipId,
    String vacatedMemberName,
    String officeName,
    String officeNameKey,
    OfficeTermReviewStatus status,
    String reason,
    OffsetDateTime decidedAt) {}
