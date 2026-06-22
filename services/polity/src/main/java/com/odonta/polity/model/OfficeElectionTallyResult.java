package com.odonta.polity.model;

import java.util.List;
import java.util.UUID;

public record OfficeElectionTallyResult(
    int eligible,
    int participation,
    int quorumRequired,
    boolean quorumMet,
    boolean decisive,
    boolean passed,
    UUID winnerMembershipId,
    String winnerName,
    OfficeElectionOutcomeReason outcomeReason,
    List<OfficeElectionCandidateTallyResult> candidates) {}
