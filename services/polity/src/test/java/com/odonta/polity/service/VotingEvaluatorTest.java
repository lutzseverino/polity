package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.evaluator.VotingEvaluator;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingOutcomeReason;
import com.odonta.polity.model.VotingThreshold;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VotingEvaluatorTest {
  private final VotingEvaluator evaluator = new VotingEvaluator();

  @Test
  void passesWithQuorumAndMoreYesThanNoVotes() {
    var result =
        evaluator.evaluate(
            procedure(),
            4,
            List.of(vote(VoteChoice.YES), vote(VoteChoice.YES), vote(VoteChoice.ABSTAIN)));

    assertThat(result.quorumRequired()).isEqualTo(2);
    assertThat(result.quorumMet()).isTrue();
    assertThat(result.thresholdMet()).isTrue();
    assertThat(result.passed()).isTrue();
    assertThat(result.outcomeReason()).isEqualTo(VotingOutcomeReason.PASSED);
  }

  @Test
  void countsAbstentionsTowardQuorumButNotTheMajority() {
    var result =
        evaluator.evaluate(
            procedure(),
            5,
            List.of(vote(VoteChoice.YES), vote(VoteChoice.NO), vote(VoteChoice.ABSTAIN)));

    assertThat(result.quorumRequired()).isEqualTo(3);
    assertThat(result.quorumMet()).isTrue();
    assertThat(result.thresholdMet()).isFalse();
    assertThat(result.passed()).isFalse();
    assertThat(result.outcomeReason()).isEqualTo(VotingOutcomeReason.THRESHOLD_NOT_MET);
  }

  @Test
  void rejectsAProposalWithoutQuorumEvenWhenEveryBallotIsYes() {
    var result = evaluator.evaluate(procedure(), 5, List.of(vote(VoteChoice.YES)));

    assertThat(result.quorumMet()).isFalse();
    assertThat(result.thresholdMet()).isTrue();
    assertThat(result.passed()).isFalse();
    assertThat(result.outcomeReason()).isEqualTo(VotingOutcomeReason.QUORUM_NOT_MET);
  }

  @Test
  void majorityOfEligibleRequiresMoreThanHalfOfAllEligibleMembers() {
    var result =
        evaluator.evaluate(
            procedure(VotingThreshold.MAJORITY_OF_ELIGIBLE),
            5,
            List.of(vote(VoteChoice.YES), vote(VoteChoice.YES), vote(VoteChoice.NO)));

    assertThat(result.quorumMet()).isTrue();
    assertThat(result.thresholdMet()).isFalse();
    assertThat(result.passed()).isFalse();
  }

  @Test
  void twoThirdsEligibleRequiresTwoThirdsOfAllEligibleMembers() {
    var result =
        evaluator.evaluate(
            procedure(VotingThreshold.TWO_THIRDS_ELIGIBLE),
            6,
            List.of(
                vote(VoteChoice.YES),
                vote(VoteChoice.YES),
                vote(VoteChoice.YES),
                vote(VoteChoice.NO),
                vote(VoteChoice.ABSTAIN)));

    assertThat(result.quorumMet()).isTrue();
    assertThat(result.thresholdMet()).isFalse();
    assertThat(result.passed()).isFalse();
  }

  private Procedure procedure() {
    return procedure(VotingThreshold.SIMPLE_MAJORITY_CAST);
  }

  private Procedure procedure(VotingThreshold threshold) {
    return new Procedure(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Procedure.ORDINARY_RESOLUTION,
        "Ordinary resolution",
        1,
        2,
        threshold,
        0,
        24,
        EffectType.ADOPT_RESOLUTION);
  }

  private Vote vote(VoteChoice choice) {
    return new Vote(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), choice, OffsetDateTime.now());
  }
}
