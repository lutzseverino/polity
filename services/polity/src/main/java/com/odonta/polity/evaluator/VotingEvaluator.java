package com.odonta.polity.evaluator;

import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingOutcomeReason;
import com.odonta.polity.model.VotingProcedure;
import com.odonta.polity.model.VotingResult;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VotingEvaluator {
  public VotingResult evaluate(VotingProcedure procedure, int eligible, List<Vote> votes) {
    return evaluateChoices(procedure, eligible, votes.stream().map(Vote::getChoice).toList());
  }

  public VotingResult evaluateChoices(
      VotingProcedure procedure, int eligible, Collection<VoteChoice> choices) {
    int yes = count(choices, VoteChoice.YES);
    int no = count(choices, VoteChoice.NO);
    int abstain = count(choices, VoteChoice.ABSTAIN);
    int participation = yes + no + abstain;
    int quorumRequired =
        (eligible * procedure.getQuorumNumerator() + procedure.getQuorumDenominator() - 1)
            / procedure.getQuorumDenominator();
    boolean quorumMet = participation >= quorumRequired;
    boolean thresholdMet = thresholdMet(procedure.getThreshold(), eligible, yes, no);
    boolean passed = quorumMet && thresholdMet;
    VotingOutcomeReason outcomeReason =
        passed
            ? VotingOutcomeReason.PASSED
            : !quorumMet
                ? VotingOutcomeReason.QUORUM_NOT_MET
                : VotingOutcomeReason.THRESHOLD_NOT_MET;
    return new VotingResult(
        eligible, yes, no, abstain, quorumRequired, quorumMet, thresholdMet, passed, outcomeReason);
  }

  private boolean thresholdMet(
      com.odonta.polity.model.VotingThreshold threshold, int eligible, int yes, int no) {
    int cast = yes + no;
    return switch (threshold) {
      case SIMPLE_MAJORITY_CAST -> yes > no;
      case MAJORITY_OF_ELIGIBLE -> yes > eligible / 2;
      case TWO_THIRDS_CAST -> cast > 0 && yes * 3 >= cast * 2;
      case TWO_THIRDS_ELIGIBLE -> yes * 3 >= eligible * 2;
      case OFFICE_ELECTION_RESULT -> false;
    };
  }

  private int count(Collection<VoteChoice> choices, VoteChoice choice) {
    return (int) choices.stream().filter(vote -> vote == choice).count();
  }
}
