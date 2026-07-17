package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CastOfficeElectionBallotRequest;
import com.odonta.polity.api.model.CreateOfficeElectionMotionRequest;
import com.odonta.polity.api.model.OfficeElectionCandidateRoundTallyResponse;
import com.odonta.polity.api.model.OfficeElectionResponse;
import com.odonta.polity.api.model.OfficeElectionRoundResponse;
import com.odonta.polity.api.model.OfficeElectionTallyResponse;
import com.odonta.polity.api.model.RespondOfficeElectionCandidacyRequest;
import com.odonta.polity.input.CastOfficeElectionBallotInput;
import com.odonta.polity.input.CreateOfficeElectionMotionInput;
import com.odonta.polity.input.RespondOfficeElectionCandidacyInput;
import com.odonta.polity.model.OfficeElectionCandidateRoundTallyResult;
import com.odonta.polity.model.OfficeElectionRoundResult;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.result.OfficeElectionResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface OfficeElectionTransportMapper {
  CreateOfficeElectionMotionInput toInput(CreateOfficeElectionMotionRequest request);

  CastOfficeElectionBallotInput toInput(CastOfficeElectionBallotRequest request);

  RespondOfficeElectionCandidacyInput toInput(RespondOfficeElectionCandidacyRequest request);

  OfficeElectionResponse toResponse(OfficeElectionResult result);

  OfficeElectionTallyResponse toResponse(OfficeElectionTallyResult result);

  OfficeElectionRoundResponse toResponse(OfficeElectionRoundResult result);

  OfficeElectionCandidateRoundTallyResponse toResponse(
      OfficeElectionCandidateRoundTallyResult result);
}
