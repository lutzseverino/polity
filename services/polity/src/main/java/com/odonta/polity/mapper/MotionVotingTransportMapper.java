package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CastVoteRequest;
import com.odonta.polity.api.model.VoteTallyResponse;
import com.odonta.polity.input.CastVoteInput;
import com.odonta.polity.model.VotingResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface MotionVotingTransportMapper {
  CastVoteInput toInput(CastVoteRequest request);

  @BeanMapping(ignoreUnmappedSourceProperties = {"thresholdMet", "passed"})
  VoteTallyResponse toResponse(VotingResult result);
}
