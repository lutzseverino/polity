package com.odonta.polity.model;

import java.math.BigDecimal;
import java.util.List;

public record OfficeElectionTallyResult(
    int eligible,
    int participation,
    int quorumRequired,
    boolean quorumMet,
    int seatsAvailable,
    int seatsFilled,
    OfficeElectionMethod method,
    BigDecimal quota,
    boolean decisive,
    boolean passed,
    OfficeElectionOutcomeReason outcomeReason,
    List<OfficeElectionCandidateTallyResult> winners,
    List<OfficeElectionCandidateTallyResult> candidates,
    List<OfficeElectionRoundResult> rounds) {}
