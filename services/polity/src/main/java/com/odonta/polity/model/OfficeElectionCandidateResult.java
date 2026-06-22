package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfficeElectionCandidateResult(
    UUID membershipId,
    String name,
    OfficeElectionCandidateStatus status,
    OffsetDateTime respondedAt) {}
