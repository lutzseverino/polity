package com.odonta.polity.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OfficeElectionRoundResult(
    int roundNumber,
    OfficeElectionRoundAction action,
    UUID candidateMembershipId,
    BigDecimal exhaustedVoteTotal,
    BigDecimal transferFactor,
    OfficeElectionTieBreakReason tieBreakReason,
    List<OfficeElectionCandidateRoundTallyResult> candidates) {}
