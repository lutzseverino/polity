package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CastOfficeElectionBallotRequest;
import com.odonta.polity.api.model.CastVoteRequest;
import com.odonta.polity.api.model.CertificationResponse;
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
import com.odonta.polity.api.model.RespondOfficeElectionCandidacyRequest;
import com.odonta.polity.api.model.VoteTallyResponse;
import com.odonta.polity.model.CastOfficeElectionBallotInput;
import com.odonta.polity.model.CastVoteInput;
import com.odonta.polity.model.CertificationResult;
import com.odonta.polity.model.CreateAppealMotionInput;
import com.odonta.polity.model.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.model.CreateConstitutionalReviewMotionInput;
import com.odonta.polity.model.CreateDisbandmentMotionInput;
import com.odonta.polity.model.CreateInstitutionChangeInput;
import com.odonta.polity.model.CreateMotionInput;
import com.odonta.polity.model.CreateOfficeChangeInput;
import com.odonta.polity.model.CreateOfficeElectionMotionInput;
import com.odonta.polity.model.CreateOfficeTermReviewMotionInput;
import com.odonta.polity.model.CreatePowerChangeInput;
import com.odonta.polity.model.CreateProcedureChangeInput;
import com.odonta.polity.model.CreateSanctionMotionInput;
import com.odonta.polity.model.MotionResult;
import com.odonta.polity.model.RespondOfficeElectionCandidacyInput;
import com.odonta.polity.model.VotingResult;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class, uses = MotionTransportConversions.class)
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

  CertificationResponse toResponse(CertificationResult certification);
}
