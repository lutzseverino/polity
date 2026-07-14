package com.odonta.polity.mapper;

import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.result.CertificationResult;
import com.odonta.polity.result.ConstitutionAmendmentProposalResult;
import com.odonta.polity.result.MotionActionAvailabilityResult;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.result.OfficeElectionResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface MotionApplicationMapper {

  @BeanMapping(
      ignoreUnmappedSourceProperties = {"constitutionVersionId", "procedureId", "introducedBy"})
  @Mapping(target = "title", source = "projection.title")
  @Mapping(target = "body", source = "projection.body")
  MotionResult toResult(
      MotionProjection projection,
      int constitutionVersion,
      String procedureName,
      String procedureNameKey,
      String introducedByName,
      VotingResult tally,
      OfficeElectionResult officeElection,
      OfficeElectionTallyResult electionTally,
      CertificationResult certification,
      VoteChoice currentVote,
      MotionActionAvailabilityResult actions,
      ConstitutionAmendmentProposalResult amendmentProposal);
}
