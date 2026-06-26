package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConstitutionalReviewResult(
    UUID id,
    UUID targetRecordId,
    int targetEntryNumber,
    OfficialRecordType targetType,
    UUID petitionerMembershipId,
    String petitionerName,
    ConstitutionalReviewStatus status,
    String reason,
    OffsetDateTime decidedAt) {}
