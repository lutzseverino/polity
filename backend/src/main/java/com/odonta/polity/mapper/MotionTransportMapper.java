package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CastVoteRequest;
import com.odonta.polity.api.model.CertificationResponse;
import com.odonta.polity.api.model.CreateAppealMotionRequest;
import com.odonta.polity.api.model.CreateConstitutionAmendmentMotionRequest;
import com.odonta.polity.api.model.CreateMotionRequest;
import com.odonta.polity.api.model.CreateOfficeAssignmentMotionRequest;
import com.odonta.polity.api.model.CreateProcedureChangeRequest;
import com.odonta.polity.api.model.CreateSanctionMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.api.model.VoteTallyResponse;
import com.odonta.polity.model.CastVoteInput;
import com.odonta.polity.model.CertificationResult;
import com.odonta.polity.model.CreateAppealMotionInput;
import com.odonta.polity.model.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.model.CreateMotionInput;
import com.odonta.polity.model.CreateOfficeAssignmentMotionInput;
import com.odonta.polity.model.CreateProcedureChangeInput;
import com.odonta.polity.model.CreateSanctionMotionInput;
import com.odonta.polity.model.MotionResult;
import com.odonta.polity.model.VotingResult;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class, uses = MotionTransportConversions.class)
public interface MotionTransportMapper {

  CreateMotionInput toInput(CreateMotionRequest request);

  CreateOfficeAssignmentMotionInput toInput(CreateOfficeAssignmentMotionRequest request);

  CreateSanctionMotionInput toInput(CreateSanctionMotionRequest request);

  CreateAppealMotionInput toInput(CreateAppealMotionRequest request);

  CreateConstitutionAmendmentMotionInput toInput(CreateConstitutionAmendmentMotionRequest request);

  CreateProcedureChangeInput toInput(CreateProcedureChangeRequest request);

  CastVoteInput toInput(CastVoteRequest request);

  MotionResponse toResponse(MotionResult result);

  List<MotionResponse> toResponses(List<MotionResult> results);

  @BeanMapping(ignoreUnmappedSourceProperties = {"thresholdMet", "passed", "explanation"})
  VoteTallyResponse toResponse(VotingResult tally);

  CertificationResponse toResponse(CertificationResult certification);
}
