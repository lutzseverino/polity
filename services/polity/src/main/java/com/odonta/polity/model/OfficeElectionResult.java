package com.odonta.polity.model;

import java.util.List;
import java.util.UUID;

public record OfficeElectionResult(
    UUID officeId,
    String officeCode,
    String officeName,
    String officeNameKey,
    List<OfficeElectionCandidateResult> candidates) {}
