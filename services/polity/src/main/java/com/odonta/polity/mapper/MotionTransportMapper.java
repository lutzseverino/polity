package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CastOfficeElectionBallotRequest;
import com.odonta.polity.api.model.CastVoteRequest;
import com.odonta.polity.api.model.CreateAppealMotionRequest;
import com.odonta.polity.api.model.CreateConstitutionAmendmentMotionRequest;
import com.odonta.polity.api.model.CreateConstitutionalReviewMotionRequest;
import com.odonta.polity.api.model.CreateDisbandmentMotionRequest;
import com.odonta.polity.api.model.CreateInstitutionChangeRequest;
import com.odonta.polity.api.model.CreateMotionRequest;
import com.odonta.polity.api.model.CreateOfficeChangeRequest;
import com.odonta.polity.api.model.CreateOfficeElectionMotionRequest;
import com.odonta.polity.api.model.CreateOfficeTermReviewMotionRequest;
import com.odonta.polity.api.model.CreatePowerChangeRequest;
import com.odonta.polity.api.model.CreateProcedureChangeRequest;
import com.odonta.polity.api.model.CreateSanctionMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.api.model.OfficeElectionCandidateRoundTallyResponse;
import com.odonta.polity.api.model.OfficeElectionRoundResponse;
import com.odonta.polity.api.model.RespondOfficeElectionCandidacyRequest;
import com.odonta.polity.api.model.VoteTallyResponse;
import com.odonta.polity.input.CastOfficeElectionBallotInput;
import com.odonta.polity.input.CastVoteInput;
import com.odonta.polity.input.CreateAppealMotionInput;
import com.odonta.polity.input.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.input.CreateConstitutionalReviewMotionInput;
import com.odonta.polity.input.CreateDisbandmentMotionInput;
import com.odonta.polity.input.CreateInstitutionChangeInput;
import com.odonta.polity.input.CreateMotionInput;
import com.odonta.polity.input.CreateOfficeChangeInput;
import com.odonta.polity.input.CreateOfficeElectionMotionInput;
import com.odonta.polity.input.CreateOfficeTermReviewMotionInput;
import com.odonta.polity.input.CreatePowerChangeInput;
import com.odonta.polity.input.CreateProcedureChangeInput;
import com.odonta.polity.input.CreateSanctionMotionInput;
import com.odonta.polity.input.RespondOfficeElectionCandidacyInput;
import com.odonta.polity.model.OfficeElectionCandidateRoundTallyResult;
import com.odonta.polity.model.OfficeElectionRoundResult;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.result.MotionResult;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    config = PolityMapperConfig.class,
    uses = {
      MotionTransportText.class,
      ActionAvailabilityTransportMapper.class,
      CertificationTransportMapper.class
    })
public interface MotionTransportMapper {

  CreateMotionInput toInput(CreateMotionRequest request);

  CreateOfficeElectionMotionInput toInput(CreateOfficeElectionMotionRequest request);

  CreateSanctionMotionInput toInput(CreateSanctionMotionRequest request);

  CreateAppealMotionInput toInput(CreateAppealMotionRequest request);

  CreateOfficeTermReviewMotionInput toInput(CreateOfficeTermReviewMotionRequest request);

  CreateConstitutionalReviewMotionInput toInput(CreateConstitutionalReviewMotionRequest request);

  CreateConstitutionAmendmentMotionInput toInput(CreateConstitutionAmendmentMotionRequest request);

  CreateInstitutionChangeInput toInput(CreateInstitutionChangeRequest request);

  CreateDisbandmentMotionInput toInput(CreateDisbandmentMotionRequest request);

  CreateProcedureChangeInput toInput(CreateProcedureChangeRequest request);

  CreateOfficeChangeInput toInput(CreateOfficeChangeRequest request);

  CreatePowerChangeInput toInput(CreatePowerChangeRequest request);

  CastVoteInput toInput(CastVoteRequest request);

  CastOfficeElectionBallotInput toInput(CastOfficeElectionBallotRequest request);

  RespondOfficeElectionCandidacyInput toInput(RespondOfficeElectionCandidacyRequest request);

  @Mapping(target = "title", source = ".", qualifiedByName = "motionTitle")
  @Mapping(target = "body", source = ".", qualifiedByName = "motionBody")
  @Mapping(target = "procedureName", source = ".", qualifiedByName = "motionProcedureName")
  MotionResponse toResponse(MotionResult result);

  List<MotionResponse> toResponses(List<MotionResult> results);

  @BeanMapping(ignoreUnmappedSourceProperties = {"thresholdMet", "passed"})
  VoteTallyResponse toResponse(VotingResult tally);

  OfficeElectionRoundResponse toResponse(OfficeElectionRoundResult round);

  OfficeElectionCandidateRoundTallyResponse toResponse(
      OfficeElectionCandidateRoundTallyResult tally);
}
