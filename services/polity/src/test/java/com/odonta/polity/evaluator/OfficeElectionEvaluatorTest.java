package com.odonta.polity.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.OfficeElectionBallotRanking;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateTallyResult;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.OfficeElectionOutcomeReason;
import com.odonta.polity.model.OfficeElectionRoundAction;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.VotingThreshold;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficeElectionEvaluatorTest {
  private final OfficeElectionEvaluator evaluator = new OfficeElectionEvaluator();

  @Test
  void rankedChoiceSingleSeatTransfersEliminatedBallotsToWinner() {
    UUID alice = UUID.randomUUID();
    UUID bruno = UUID.randomUUID();
    UUID clara = UUID.randomUUID();

    var result =
        evaluator.evaluate(
            procedure(),
            5,
            1,
            OfficeElectionMethod.RANKED_CHOICE,
            candidates(
                candidate(alice, "Alice"), candidate(bruno, "Bruno"), candidate(clara, "Clara")),
            List.of(
                ballot(alice),
                ballot(alice),
                ballot(bruno),
                ballot(bruno),
                ballot(clara, bruno, alice)));

    assertThat(result.passed()).isTrue();
    assertThat(result.seatsFilled()).isEqualTo(1);
    assertThat(result.winners())
        .extracting(OfficeElectionCandidateTallyResult::membershipId)
        .containsExactly(bruno);
    assertThat(result.rounds())
        .extracting(round -> round.action())
        .containsExactly(OfficeElectionRoundAction.ELIMINATED, OfficeElectionRoundAction.ELECTED);
  }

  @Test
  void rankedChoiceSingleSeatUsesDeterministicTieBreakWhenBallotsExhaust() {
    UUID alice = UUID.randomUUID();
    UUID bruno = UUID.randomUUID();

    var result =
        evaluator.evaluate(
            procedure(),
            3,
            1,
            OfficeElectionMethod.RANKED_CHOICE,
            candidates(candidate(alice, "Alice"), candidate(bruno, "Bruno")),
            List.of(ballot(alice), ballot(bruno)));

    assertThat(result.passed()).isTrue();
    assertThat(result.winners())
        .extracting(OfficeElectionCandidateTallyResult::membershipId)
        .containsExactly(alice.toString().compareTo(bruno.toString()) < 0 ? alice : bruno);
  }

  @Test
  void rankedChoiceMultiSeatUsesStvQuotaAndFillsSeats() {
    UUID alice = UUID.randomUUID();
    UUID bruno = UUID.randomUUID();
    UUID clara = UUID.randomUUID();
    UUID dario = UUID.randomUUID();

    var result =
        evaluator.evaluate(
            procedure(),
            7,
            2,
            OfficeElectionMethod.RANKED_CHOICE,
            candidates(
                candidate(alice, "Alice"),
                candidate(bruno, "Bruno"),
                candidate(clara, "Clara"),
                candidate(dario, "Dario")),
            List.of(
                ballot(alice, bruno),
                ballot(alice, bruno),
                ballot(alice, clara),
                ballot(alice, dario),
                ballot(bruno, alice),
                ballot(clara, bruno),
                ballot(dario, bruno)));

    assertThat(result.passed()).isTrue();
    assertThat(result.quota()).isEqualByComparingTo("3");
    assertThat(result.seatsAvailable()).isEqualTo(2);
    assertThat(result.seatsFilled()).isEqualTo(2);
    assertThat(result.winners())
        .extracting(OfficeElectionCandidateTallyResult::membershipId)
        .containsExactly(alice, bruno);
    assertThat(result.rounds())
        .extracting(round -> round.action())
        .contains(OfficeElectionRoundAction.ELECTED);
  }

  @Test
  void rejectsWhenNoSeatIsAvailable() {
    UUID alice = UUID.randomUUID();

    var result =
        evaluator.evaluate(
            procedure(),
            1,
            0,
            OfficeElectionMethod.RANKED_CHOICE,
            candidates(candidate(alice, "Alice")),
            List.of(ballot(alice)));

    assertThat(result.passed()).isFalse();
    assertThat(result.seatsFilled()).isZero();
    assertThat(result.winners()).isEmpty();
    assertThat(result.outcomeReason()).isEqualTo(OfficeElectionOutcomeReason.NO_DECISIVE_RESULT);
  }

  private Procedure procedure() {
    return new Procedure(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Procedure.OFFICE_ELECTION,
        "Office election",
        1,
        2,
        VotingThreshold.OFFICE_ELECTION_RESULT,
        0,
        24,
        EffectType.ELECT_OFFICE);
  }

  private List<OfficeElectionCandidateOption> candidates(
      OfficeElectionCandidateOption... candidates) {
    return List.of(candidates);
  }

  private OfficeElectionCandidateOption candidate(UUID membershipId, String name) {
    return new OfficeElectionCandidateOption(membershipId, name);
  }

  private OfficeElectionBallotRanking ballot(UUID... candidateMembershipIds) {
    return new OfficeElectionBallotRanking(UUID.randomUUID(), List.of(candidateMembershipIds));
  }
}
