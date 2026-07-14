package com.odonta.polity.result;

import com.odonta.polity.model.OfficeElectionCandidateStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OfficeElectionCandidateResult(
    UUID membershipId,
    String name,
    OfficeElectionCandidateStatus status,
    OffsetDateTime respondedAt) {}
