package com.odonta.polity.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OfficeElectionCandidateTallyResult(
    UUID membershipId, String name, BigDecimal voteTotal) {}
