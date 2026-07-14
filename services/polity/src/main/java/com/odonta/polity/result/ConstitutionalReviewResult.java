package com.odonta.polity.result;

import com.odonta.polity.model.ConstitutionalReviewStatus;
import com.odonta.polity.model.OfficialRecordType;
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
