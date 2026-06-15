package com.odonta.polity.service;

import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingProcedure;
import com.odonta.polity.model.VotingResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VotingEvaluator {
  public VotingResult evaluate(VotingProcedure procedure, int eligible, List<Vote> votes) {
    int yes = count(votes, VoteChoice.YES);
    int no = count(votes, VoteChoice.NO);
    int abstain = count(votes, VoteChoice.ABSTAIN);
    int participation = yes + no + abstain;
    int quorumRequired =
        (eligible * procedure.getQuorumNumerator() + procedure.getQuorumDenominator() - 1)
            / procedure.getQuorumDenominator();
    boolean quorumMet = participation >= quorumRequired;
    boolean thresholdMet = yes > no;
    boolean passed = quorumMet && thresholdMet;
    String explanation =
        "%d of %d eligible members participated; quorum required %d. The vote was %d yes, %d no, and %d abstain. %s"
            .formatted(
                participation,
                eligible,
                quorumRequired,
                yes,
                no,
                abstain,
                passed
                    ? "Quorum and simple majority were satisfied."
                    : !quorumMet
                        ? "The motion failed because quorum was not satisfied."
                        : "The motion failed because yes votes did not exceed no votes.");
    return new VotingResult(
        eligible, yes, no, abstain, quorumRequired, quorumMet, thresholdMet, passed, explanation);
  }

  private int count(List<Vote> votes, VoteChoice choice) {
    return (int) votes.stream().filter(vote -> vote.getChoice() == choice).count();
  }
}
