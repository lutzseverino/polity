package com.odonta.polity.evaluator;

import com.odonta.polity.model.OfficeElectionBallotRanking;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateRoundTallyResult;
import com.odonta.polity.model.OfficeElectionCandidateTallyResult;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.OfficeElectionOutcomeReason;
import com.odonta.polity.model.OfficeElectionRoundAction;
import com.odonta.polity.model.OfficeElectionRoundResult;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.OfficeElectionTieBreakReason;
import com.odonta.polity.model.VotingProcedure;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OfficeElectionEvaluator {
  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final MathContext MATH = MathContext.DECIMAL128;

  public OfficeElectionTallyResult evaluate(
      VotingProcedure procedure,
      int eligible,
      int seatsAvailable,
      OfficeElectionMethod method,
      List<OfficeElectionCandidateOption> candidates,
      List<OfficeElectionBallotRanking> ballots) {
    Map<UUID, OfficeElectionCandidateOption> candidatesById =
        candidates.stream()
            .collect(
                Collectors.toMap(
                    OfficeElectionCandidateOption::membershipId,
                    Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new));
    List<OfficeElectionBallotRanking> castBallots =
        ballots.stream()
            .filter(ballot -> ballot.candidateMembershipIds() != null)
            .filter(ballot -> !ballot.candidateMembershipIds().isEmpty())
            .toList();
    List<OfficeElectionBallotRanking> validBallots =
        castBallots.stream().map(ballot -> validRanking(ballot, candidatesById.keySet())).toList();
    int participation = castBallots.size();
    int quorumRequired =
        (eligible * procedure.getQuorumNumerator() + procedure.getQuorumDenominator() - 1)
            / procedure.getQuorumDenominator();
    boolean quorumMet = participation >= quorumRequired;
    Count count =
        method == OfficeElectionMethod.PLURALITY
            ? plurality(seatsAvailable, candidatesById, validBallots)
            : ranked(seatsAvailable, candidatesById, validBallots);
    boolean decisive = !count.winners().isEmpty();
    boolean passed = quorumMet && decisive;
    OfficeElectionOutcomeReason outcomeReason =
        passed
            ? OfficeElectionOutcomeReason.PASSED
            : !quorumMet
                ? OfficeElectionOutcomeReason.QUORUM_NOT_MET
                : OfficeElectionOutcomeReason.NO_DECISIVE_RESULT;
    return new OfficeElectionTallyResult(
        eligible,
        participation,
        quorumRequired,
        quorumMet,
        seatsAvailable,
        passed ? count.winners().size() : 0,
        method,
        count.quota(),
        decisive,
        passed,
        outcomeReason,
        passed ? count.winners() : List.of(),
        count.candidates(),
        count.rounds());
  }

  private Count plurality(
      int seatsAvailable,
      Map<UUID, OfficeElectionCandidateOption> candidatesById,
      List<OfficeElectionBallotRanking> ballots) {
    Map<UUID, BigDecimal> totals = zeroTotals(candidatesById.keySet());
    Map<UUID, Integer> firstPreferences = firstPreferences(candidatesById.keySet(), ballots);
    ballots.stream()
        .map(OfficeElectionBallotRanking::candidateMembershipIds)
        .filter(ranking -> !ranking.isEmpty())
        .map(List::getFirst)
        .forEach(
            candidate -> totals.compute(candidate, (id, total) -> valueOrZero(total).add(ONE)));
    List<UUID> ordered =
        candidatesById.keySet().stream()
            .sorted(
                (left, right) ->
                    compareCandidates(right, left, totals, List.of(), firstPreferences))
            .toList();
    List<OfficeElectionCandidateTallyResult> winners =
        ordered.stream()
            .filter(candidate -> totals.get(candidate).compareTo(BigDecimal.ZERO) > 0)
            .limit(Math.max(0, seatsAvailable))
            .map(candidate -> candidateResult(candidate, candidatesById, totals))
            .toList();
    List<OfficeElectionRoundResult> rounds =
        List.of(
            new OfficeElectionRoundResult(
                1,
                winners.isEmpty()
                    ? OfficeElectionRoundAction.NO_COUNTABLE_BALLOTS
                    : OfficeElectionRoundAction.ELECTED,
                winners.isEmpty() ? null : winners.getFirst().membershipId(),
                BigDecimal.ZERO,
                null,
                null,
                roundTallies(totals, candidatesById, firstPreferences)));
    return new Count(
        null, winners, finalCandidates(candidatesById, totals, firstPreferences, winners), rounds);
  }

  private Count ranked(
      int seatsAvailable,
      Map<UUID, OfficeElectionCandidateOption> candidatesById,
      List<OfficeElectionBallotRanking> ballots) {
    return seatsAvailable == 1
        ? instantRunoff(candidatesById, ballots)
        : singleTransferableVote(seatsAvailable, candidatesById, ballots);
  }

  private Count instantRunoff(
      Map<UUID, OfficeElectionCandidateOption> candidatesById,
      List<OfficeElectionBallotRanking> ballots) {
    Set<UUID> continuing = new LinkedHashSet<>(candidatesById.keySet());
    Map<UUID, Integer> firstPreferences = firstPreferences(candidatesById.keySet(), ballots);
    List<Map<UUID, BigDecimal>> history = new ArrayList<>();
    List<OfficeElectionRoundResult> rounds = new ArrayList<>();
    List<OfficeElectionCandidateTallyResult> winners = List.of();
    Map<UUID, BigDecimal> totals = zeroTotals(candidatesById.keySet());
    int roundNumber = 1;
    while (!continuing.isEmpty()) {
      Tally tally = tally(ballots, continuing);
      totals = withAllCandidates(candidatesById.keySet(), tally.totals());
      BigDecimal exhausted = tally.exhausted();
      history.add(totals);
      BigDecimal activeTotal = BigDecimal.valueOf(ballots.size()).subtract(exhausted);
      if (activeTotal.compareTo(BigDecimal.ZERO) <= 0) {
        rounds.add(
            round(
                roundNumber,
                OfficeElectionRoundAction.NO_COUNTABLE_BALLOTS,
                null,
                exhausted,
                null,
                null,
                totals,
                candidatesById,
                firstPreferences));
        break;
      }
      UUID leader = strongest(continuing, totals, history, firstPreferences);
      if (totals.get(leader).multiply(BigDecimal.valueOf(2)).compareTo(activeTotal) > 0
          || continuing.size() == 1) {
        winners = List.of(candidateResult(leader, candidatesById, totals));
        rounds.add(
            round(
                roundNumber,
                OfficeElectionRoundAction.ELECTED,
                leader,
                exhausted,
                null,
                null,
                totals,
                candidatesById,
                firstPreferences));
        break;
      }
      UUID eliminated = weakest(continuing, totals, history, firstPreferences);
      rounds.add(
          round(
              roundNumber,
              OfficeElectionRoundAction.ELIMINATED,
              eliminated,
              exhausted,
              null,
              tieBreakReason(continuing, totals, eliminated),
              totals,
              candidatesById,
              firstPreferences));
      continuing.remove(eliminated);
      roundNumber++;
    }
    return new Count(
        null, winners, finalCandidates(candidatesById, totals, firstPreferences, winners), rounds);
  }

  private Count singleTransferableVote(
      int seatsAvailable,
      Map<UUID, OfficeElectionCandidateOption> candidatesById,
      List<OfficeElectionBallotRanking> ballots) {
    BigDecimal quota = droopQuota(ballots.size(), seatsAvailable);
    Set<UUID> continuing = new LinkedHashSet<>(candidatesById.keySet());
    Map<UUID, Integer> firstPreferences = firstPreferences(candidatesById.keySet(), ballots);
    List<WeightedBallot> weightedBallots =
        ballots.stream().map(ballot -> new WeightedBallot(ballot, ONE)).toList();
    List<OfficeElectionCandidateTallyResult> winners = new ArrayList<>();
    List<Map<UUID, BigDecimal>> history = new ArrayList<>();
    List<OfficeElectionRoundResult> rounds = new ArrayList<>();
    Map<UUID, BigDecimal> totals = zeroTotals(candidatesById.keySet());
    int roundNumber = 1;
    while (!continuing.isEmpty() && winners.size() < seatsAvailable) {
      Tally tally = tallyWeightedBallots(weightedBallots, continuing);
      totals = withAllCandidates(candidatesById.keySet(), tally.totals());
      BigDecimal exhausted = tally.exhausted();
      history.add(totals);
      Map<UUID, BigDecimal> roundTotals = totals;
      int seatsRemaining = seatsAvailable - winners.size();
      List<UUID> countableContinuing =
          continuing.stream()
              .filter(candidate -> roundTotals.get(candidate).compareTo(BigDecimal.ZERO) > 0)
              .toList();
      if (countableContinuing.isEmpty()) {
        rounds.add(
            round(
                roundNumber,
                OfficeElectionRoundAction.NO_COUNTABLE_BALLOTS,
                null,
                exhausted,
                null,
                null,
                totals,
                candidatesById,
                firstPreferences));
        break;
      }
      if (countableContinuing.size() <= seatsRemaining) {
        List<UUID> elected =
            countableContinuing.stream()
                .sorted(
                    (left, right) ->
                        compareCandidates(right, left, roundTotals, history, firstPreferences))
                .toList();
        elected.stream()
            .map(candidate -> candidateResult(candidate, candidatesById, roundTotals))
            .forEach(winners::add);
        rounds.add(
            round(
                roundNumber,
                OfficeElectionRoundAction.ELECTED_REMAINING,
                elected.getFirst(),
                exhausted,
                null,
                null,
                totals,
                candidatesById,
                firstPreferences));
        break;
      }
      UUID elected =
          countableContinuing.stream()
              .filter(candidate -> roundTotals.get(candidate).compareTo(quota) >= 0)
              .max(
                  (left, right) ->
                      compareCandidates(left, right, roundTotals, history, firstPreferences))
              .orElse(null);
      if (elected != null) {
        BigDecimal total = totals.get(elected);
        BigDecimal surplus = total.subtract(quota);
        BigDecimal transferFactor =
            total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : surplus.divide(total, MATH).max(BigDecimal.ZERO);
        winners.add(candidateResult(elected, candidatesById, totals));
        rounds.add(
            round(
                roundNumber,
                OfficeElectionRoundAction.ELECTED,
                elected,
                exhausted,
                transferFactor,
                tieBreakReason(countableContinuing, totals, elected),
                totals,
                candidatesById,
                firstPreferences));
        continuing.remove(elected);
        weightedBallots = transferSurplus(weightedBallots, continuing, elected, transferFactor);
      } else {
        UUID eliminated = weakest(countableContinuing, totals, history, firstPreferences);
        rounds.add(
            round(
                roundNumber,
                OfficeElectionRoundAction.ELIMINATED,
                eliminated,
                exhausted,
                null,
                tieBreakReason(countableContinuing, totals, eliminated),
                totals,
                candidatesById,
                firstPreferences));
        continuing.remove(eliminated);
      }
      roundNumber++;
    }
    return new Count(
        quota, winners, finalCandidates(candidatesById, totals, firstPreferences, winners), rounds);
  }

  private List<WeightedBallot> transferSurplus(
      List<WeightedBallot> ballots, Set<UUID> continuing, UUID elected, BigDecimal transferFactor) {
    return ballots.stream()
        .map(
            ballot ->
                elected.equals(firstContinuingPreference(ballot.ranking(), continuing, elected))
                    ? new WeightedBallot(
                        ballot.ranking(), ballot.value().multiply(transferFactor, MATH))
                    : ballot)
        .toList();
  }

  private Tally tally(List<OfficeElectionBallotRanking> ballots, Set<UUID> continuing) {
    return tallyWeightedBallots(
        ballots.stream().map(ballot -> new WeightedBallot(ballot, ONE)).toList(), continuing);
  }

  private Tally tallyWeightedBallots(List<WeightedBallot> ballots, Set<UUID> continuing) {
    Map<UUID, BigDecimal> totals = new LinkedHashMap<>();
    BigDecimal exhausted = BigDecimal.ZERO;
    for (WeightedBallot ballot : ballots) {
      UUID candidate = firstContinuingPreference(ballot.ranking(), continuing);
      if (candidate == null) {
        exhausted = exhausted.add(ballot.value(), MATH);
      } else {
        totals.compute(candidate, (id, total) -> valueOrZero(total).add(ballot.value(), MATH));
      }
    }
    return new Tally(totals, exhausted);
  }

  private UUID firstContinuingPreference(
      OfficeElectionBallotRanking ranking, Set<UUID> continuing, UUID includedCandidate) {
    for (UUID candidate : ranking.candidateMembershipIds()) {
      if (candidate.equals(includedCandidate) || continuing.contains(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private UUID firstContinuingPreference(
      OfficeElectionBallotRanking ranking, Set<UUID> continuing) {
    for (UUID candidate : ranking.candidateMembershipIds()) {
      if (continuing.contains(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private BigDecimal droopQuota(int countableBallots, int seatsAvailable) {
    if (seatsAvailable <= 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(Math.floorDiv(countableBallots, seatsAvailable + 1) + 1L);
  }

  private OfficeElectionBallotRanking validRanking(
      OfficeElectionBallotRanking ballot, Set<UUID> candidateIds) {
    List<UUID> ranking =
        ballot.candidateMembershipIds().stream().filter(candidateIds::contains).distinct().toList();
    return new OfficeElectionBallotRanking(ballot.membershipId(), ranking);
  }

  private Map<UUID, Integer> firstPreferences(
      Set<UUID> candidateIds, List<OfficeElectionBallotRanking> ballots) {
    Map<UUID, Integer> totals =
        candidateIds.stream().collect(Collectors.toMap(Function.identity(), id -> 0));
    ballots.stream()
        .map(OfficeElectionBallotRanking::candidateMembershipIds)
        .filter(ranking -> !ranking.isEmpty())
        .map(List::getFirst)
        .forEach(candidate -> totals.compute(candidate, (id, total) -> valueOrZero(total) + 1));
    return totals;
  }

  private List<OfficeElectionCandidateTallyResult> finalCandidates(
      Map<UUID, OfficeElectionCandidateOption> candidatesById,
      Map<UUID, BigDecimal> totals,
      Map<UUID, Integer> firstPreferences,
      List<OfficeElectionCandidateTallyResult> winners) {
    Set<UUID> winnerIds =
        winners.stream()
            .map(OfficeElectionCandidateTallyResult::membershipId)
            .collect(Collectors.toSet());
    return candidatesById.keySet().stream()
        .map(candidate -> candidateResult(candidate, candidatesById, totals))
        .sorted(
            Comparator.comparing(
                    (OfficeElectionCandidateTallyResult candidate) ->
                        !winnerIds.contains(candidate.membershipId()))
                .thenComparing(
                    OfficeElectionCandidateTallyResult::voteTotal, Comparator.reverseOrder())
                .thenComparing(candidate -> candidate.membershipId().toString()))
        .toList();
  }

  private OfficeElectionCandidateTallyResult candidateResult(
      UUID candidate,
      Map<UUID, OfficeElectionCandidateOption> candidatesById,
      Map<UUID, BigDecimal> totals) {
    return new OfficeElectionCandidateTallyResult(
        candidate,
        candidatesById.get(candidate).name(),
        normalize(valueOrZero(totals.get(candidate))));
  }

  private OfficeElectionRoundResult round(
      int roundNumber,
      OfficeElectionRoundAction action,
      UUID candidateMembershipId,
      BigDecimal exhausted,
      BigDecimal transferFactor,
      OfficeElectionTieBreakReason tieBreakReason,
      Map<UUID, BigDecimal> totals,
      Map<UUID, OfficeElectionCandidateOption> candidatesById,
      Map<UUID, Integer> firstPreferences) {
    return new OfficeElectionRoundResult(
        roundNumber,
        action,
        candidateMembershipId,
        normalize(exhausted),
        transferFactor == null ? null : normalize(transferFactor),
        tieBreakReason,
        roundTallies(totals, candidatesById, firstPreferences));
  }

  private List<OfficeElectionCandidateRoundTallyResult> roundTallies(
      Map<UUID, BigDecimal> totals,
      Map<UUID, OfficeElectionCandidateOption> candidatesById,
      Map<UUID, Integer> firstPreferences) {
    return candidatesById.keySet().stream()
        .map(
            candidate ->
                new OfficeElectionCandidateRoundTallyResult(
                    candidate,
                    candidatesById.get(candidate).name(),
                    normalize(valueOrZero(totals.get(candidate)))))
        .sorted(
            (left, right) ->
                compareCandidates(
                    right.membershipId(), left.membershipId(), totals, List.of(), firstPreferences))
        .toList();
  }

  private UUID strongest(
      Set<UUID> candidates,
      Map<UUID, BigDecimal> currentTotals,
      List<Map<UUID, BigDecimal>> history,
      Map<UUID, Integer> firstPreferences) {
    return candidates.stream()
        .max(
            (left, right) ->
                compareCandidates(left, right, currentTotals, history, firstPreferences))
        .orElseThrow();
  }

  private UUID weakest(
      List<UUID> candidates,
      Map<UUID, BigDecimal> currentTotals,
      List<Map<UUID, BigDecimal>> history,
      Map<UUID, Integer> firstPreferences) {
    return candidates.stream()
        .min(
            (left, right) ->
                compareCandidates(left, right, currentTotals, history, firstPreferences))
        .orElseThrow();
  }

  private UUID weakest(
      Set<UUID> candidates,
      Map<UUID, BigDecimal> currentTotals,
      List<Map<UUID, BigDecimal>> history,
      Map<UUID, Integer> firstPreferences) {
    return weakest(new ArrayList<>(candidates), currentTotals, history, firstPreferences);
  }

  private int compareCandidates(
      UUID left,
      UUID right,
      Map<UUID, BigDecimal> currentTotals,
      List<Map<UUID, BigDecimal>> history,
      Map<UUID, Integer> firstPreferences) {
    int current =
        valueOrZero(currentTotals.get(left)).compareTo(valueOrZero(currentTotals.get(right)));
    if (current != 0) {
      return current;
    }
    for (int index = history.size() - 2; index >= 0; index--) {
      int previous =
          valueOrZero(history.get(index).get(left))
              .compareTo(valueOrZero(history.get(index).get(right)));
      if (previous != 0) {
        return previous;
      }
    }
    int firstPreference =
        Integer.compare(
            firstPreferences.getOrDefault(left, 0), firstPreferences.getOrDefault(right, 0));
    if (firstPreference != 0) {
      return firstPreference;
    }
    return right.toString().compareTo(left.toString());
  }

  private OfficeElectionTieBreakReason tieBreakReason(
      Iterable<UUID> candidates, Map<UUID, BigDecimal> totals, UUID selected) {
    BigDecimal selectedTotal = valueOrZero(totals.get(selected));
    for (UUID candidate : candidates) {
      if (!candidate.equals(selected)
          && valueOrZero(totals.get(candidate)).compareTo(selectedTotal) == 0) {
        return OfficeElectionTieBreakReason.DETERMINISTIC;
      }
    }
    return null;
  }

  private Map<UUID, BigDecimal> zeroTotals(Set<UUID> candidateIds) {
    return candidateIds.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                id -> BigDecimal.ZERO,
                (left, right) -> left,
                LinkedHashMap::new));
  }

  private Map<UUID, BigDecimal> withAllCandidates(
      Set<UUID> candidateIds, Map<UUID, BigDecimal> totals) {
    Map<UUID, BigDecimal> result = zeroTotals(candidateIds);
    result.putAll(totals);
    return result;
  }

  private BigDecimal valueOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private int valueOrZero(Integer value) {
    return value == null ? 0 : value;
  }

  private BigDecimal normalize(BigDecimal value) {
    return value.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
  }

  private record WeightedBallot(OfficeElectionBallotRanking ranking, BigDecimal value) {}

  private record Tally(Map<UUID, BigDecimal> totals, BigDecimal exhausted) {}

  private record Count(
      BigDecimal quota,
      List<OfficeElectionCandidateTallyResult> winners,
      List<OfficeElectionCandidateTallyResult> candidates,
      List<OfficeElectionRoundResult> rounds) {}
}
