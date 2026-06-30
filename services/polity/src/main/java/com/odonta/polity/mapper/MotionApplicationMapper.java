package com.odonta.polity.mapper;

import com.odonta.polity.model.Certification;
import com.odonta.polity.model.CertificationResult;
import com.odonta.polity.model.MotionResult;
import com.odonta.polity.model.OfficeElectionBallotResult;
import com.odonta.polity.model.OfficeElectionCandidateResult;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.OfficeElectionResult;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.repository.MotionProjection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface MotionApplicationMapper {

  @BeanMapping(
      ignoreUnmappedSourceProperties = {
        "createdAt",
        "updatedAt",
        "id",
        "polityId",
        "motionId",
        "requestedBy",
        "modality",
        "eligibleCount",
        "yesCount",
        "noCount",
        "abstainCount",
        "electionParticipationCount",
        "electionDecisive",
        "electionWinnerCount",
        "electionTallySnapshot",
        "quorumRequired",
        "quorumMet",
        "thresholdMet"
      })
  CertificationResult toResult(Certification certification);

  @BeanMapping(
      ignoreUnmappedSourceProperties = {"constitutionVersionId", "procedureId", "introducedBy"})
  MotionResult toResult(
      MotionProjection projection,
      int constitutionVersion,
      String procedureName,
      String procedureNameKey,
      String introducedByName,
      VotingResult tally,
      OfficeElectionResult officeElection,
      OfficeElectionTallyResult electionTally,
      CertificationResult certification);

  OfficeElectionCandidateResult toCandidateResult(
      UUID membershipId,
      String name,
      OfficeElectionCandidateStatus status,
      OffsetDateTime respondedAt);

  OfficeElectionResult toOfficeElectionResult(
      UUID officeId,
      String officeCode,
      String officeName,
      String officeNameKey,
      int seatsAvailable,
      OfficeElectionMethod method,
      OfficeElectionBallotResult currentBallot,
      List<OfficeElectionCandidateResult> candidates);
}
