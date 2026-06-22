package com.odonta.polity.evaluator;

import com.odonta.polity.model.OfficeElectionBallot;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateTallyResult;
import com.odonta.polity.model.OfficeElectionOutcomeReason;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.VotingProcedure;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OfficeElectionEvaluator {
  public OfficeElectionTallyResult evaluate(
      VotingProcedure procedure,
      int eligible,
      List<OfficeElectionCandidateOption> candidates,
      List<OfficeElectionBallot> ballots) {
    Set<UUID> candidateIds =
        candidates.stream()
            .map(OfficeElectionCandidateOption::membershipId)
            .collect(Collectors.toSet());
    List<OfficeElectionBallot> validBallots =
        ballots.stream()
            .filter(ballot -> candidateIds.contains(ballot.getCandidateMembershipId()))
            .toList();
    Map<UUID, Long> ballotCounts =
        validBallots.stream()
            .collect(
                Collectors.groupingBy(
                    OfficeElectionBallot::getCandidateMembershipId, Collectors.counting()));
    int participation = validBallots.size();
    int quorumRequired =
        (eligible * procedure.getQuorumNumerator() + procedure.getQuorumDenominator() - 1)
            / procedure.getQuorumDenominator();
    boolean quorumMet = participation >= quorumRequired;
    List<OfficeElectionCandidateTallyResult> tallies =
        candidates.stream()
            .map(
                candidate ->
                    new OfficeElectionCandidateTallyResult(
                        candidate.membershipId(),
                        candidate.name(),
                        Math.toIntExact(ballotCounts.getOrDefault(candidate.membershipId(), 0L))))
            .sorted(
                Comparator.comparingInt(OfficeElectionCandidateTallyResult::ballots)
                    .reversed()
                    .thenComparing(OfficeElectionCandidateTallyResult::name))
            .toList();
    int highScore = tallies.isEmpty() ? 0 : tallies.getFirst().ballots();
    List<OfficeElectionCandidateTallyResult> leaders =
        tallies.stream().filter(candidate -> candidate.ballots() == highScore).toList();
    boolean decisive = highScore > 0 && leaders.size() == 1;
    boolean passed = quorumMet && decisive;
    OfficeElectionCandidateTallyResult winner = passed ? leaders.getFirst() : null;
    OfficeElectionOutcomeReason outcomeReason =
        passed
            ? OfficeElectionOutcomeReason.PASSED
            : !quorumMet
                ? OfficeElectionOutcomeReason.QUORUM_NOT_MET
                : OfficeElectionOutcomeReason.NO_DECISIVE_PLURALITY;
    return new OfficeElectionTallyResult(
        eligible,
        participation,
        quorumRequired,
        quorumMet,
        decisive,
        passed,
        winner == null ? null : winner.membershipId(),
        winner == null ? null : winner.name(),
        outcomeReason,
        tallies);
  }
}
