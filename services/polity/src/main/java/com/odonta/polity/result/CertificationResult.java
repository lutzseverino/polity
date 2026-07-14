package com.odonta.polity.result;

import com.odonta.polity.model.CertificationModality;
import com.odonta.polity.model.CertificationOutcomeReason;
import java.time.OffsetDateTime;

public record CertificationResult(
    CertificationModality modality,
    int eligibleCount,
    Integer yesCount,
    Integer noCount,
    Integer abstainCount,
    Integer electionParticipationCount,
    Boolean electionDecisive,
    Integer electionWinnerCount,
    int quorumRequired,
    boolean quorumMet,
    boolean thresholdMet,
    boolean passed,
    CertificationOutcomeReason outcomeReason,
    OffsetDateTime certifiedAt) {}
