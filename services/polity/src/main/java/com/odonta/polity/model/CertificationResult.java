package com.odonta.polity.model;

import java.time.OffsetDateTime;

public record CertificationResult(
    boolean passed, CertificationOutcomeReason outcomeReason, OffsetDateTime certifiedAt) {}
