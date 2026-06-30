package com.odonta.polity.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OfficeElectionCandidateRoundTallyResult(
    UUID membershipId, String name, BigDecimal voteTotal) {}
