package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CreateMotionRequest;
import com.odonta.polity.api.model.MotionResponse;
import com.odonta.polity.input.CreateMotionInput;
import com.odonta.polity.result.MotionResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    config = PolityMapperConfig.class,
    uses = {
      MotionTransportText.class,
      MotionVotingTransportMapper.class,
      OfficeElectionTransportMapper.class,
      ConstitutionAmendmentTransportMapper.class,
      ActionAvailabilityTransportMapper.class,
      CertificationTransportMapper.class
    })
public interface MotionTransportMapper {

  CreateMotionInput toInput(CreateMotionRequest request);

  @Mapping(target = "title", source = ".", qualifiedByName = "motionTitle")
  @Mapping(target = "body", source = ".", qualifiedByName = "motionBody")
  @Mapping(target = "procedureName", source = ".", qualifiedByName = "motionProcedureName")
  MotionResponse toResponse(MotionResult result);

  List<MotionResponse> toResponses(List<MotionResult> results);
}
