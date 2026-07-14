package com.odonta.polity.result;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingResult;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record MotionResult(
    UUID id,
    String title,
    String body,
    String titleKey,
    String bodyKey,
    Map<String, Object> templateParams,
    MotionStatus status,
    EffectType effectType,
    int constitutionVersion,
    String procedureName,
    String procedureNameKey,
    String introducedByName,
    OffsetDateTime openedAt,
    OffsetDateTime votingOpensAt,
    OffsetDateTime votingClosesAt,
    OffsetDateTime certificationOpensAt,
    VotingResult tally,
    OfficeElectionResult officeElection,
    OfficeElectionTallyResult electionTally,
    CertificationResult certification,
    VoteChoice currentVote,
    MotionActionAvailabilityResult actions,
    ConstitutionAmendmentProposalResult amendmentProposal) {
  public MotionResult {
    templateParams = TemplateParameters.copyOf(templateParams);
  }
}
