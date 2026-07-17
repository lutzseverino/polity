package com.odonta.polity.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.api.model.CastVoteRequest;
import com.odonta.polity.input.CastVoteInput;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingOutcomeReason;
import com.odonta.polity.model.VotingResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class MotionVotingTransportMapperTest {
  private final MotionVotingTransportMapper mapper =
      Mappers.getMapper(MotionVotingTransportMapper.class);

  @Test
  void mapsVoteCommandsAtTheMotionVotingBoundary() {
    CastVoteInput input =
        mapper.toInput(new CastVoteRequest(com.odonta.polity.api.model.VoteChoice.ABSTAIN));

    assertThat(input.choice()).isEqualTo(VoteChoice.ABSTAIN);
  }

  @Test
  void mapsPublishedTallyFieldsWithoutLeakingInternalDecisionFlags() {
    var response =
        mapper.toResponse(
            new VotingResult(12, 7, 3, 2, 8, true, true, true, VotingOutcomeReason.PASSED));

    assertThat(response.getEligible()).isEqualTo(12);
    assertThat(response.getYes()).isEqualTo(7);
    assertThat(response.getNo()).isEqualTo(3);
    assertThat(response.getAbstain()).isEqualTo(2);
    assertThat(response.getQuorumRequired()).isEqualTo(8);
    assertThat(response.getQuorumMet()).isTrue();
    assertThat(response.getOutcomeReason())
        .isEqualTo(com.odonta.polity.api.model.VotingOutcomeReason.PASSED);
  }
}
