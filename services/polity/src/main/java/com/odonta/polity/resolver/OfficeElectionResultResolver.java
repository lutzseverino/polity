package com.odonta.polity.resolver;

import com.odonta.common.api.ApiException;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.mapper.OfficeElectionApplicationMapper;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.OfficeElectionBallotRanking;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.repository.CertificationProjection;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.OfficeElectionBallotPreferenceProjection;
import com.odonta.polity.repository.OfficeElectionBallotPreferenceRepository;
import com.odonta.polity.repository.OfficeElectionBallotProjection;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateProjection;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalProjection;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeProjection;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureProjection;
import com.odonta.polity.result.OfficeElectionBallotResult;
import com.odonta.polity.result.OfficeElectionCandidateResult;
import com.odonta.polity.service.MembershipService;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficeElectionResultResolver {
  private final OfficeElectionApplicationMapper mapper;
  private final OfficeElectionEvaluator evaluator;
  private final OfficeElectionProposalRepository proposals;
  private final OfficeElectionCandidateRepository candidates;
  private final OfficeElectionBallotRepository ballots;
  private final OfficeElectionBallotPreferenceRepository preferences;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final MembershipRepository memberships;
  private final MembershipService membershipService;

  public Map<UUID, OfficeElectionResolution> resolveAll(
      UUID polityId,
      List<MotionProjection> motions,
      UUID currentMembershipId,
      Map<UUID, ProcedureProjection> procedures,
      Map<UUID, Integer> electorCounts,
      Map<UUID, CertificationProjection> certifications,
      OffsetDateTime now) {
    List<UUID> motionIds = motions.stream().map(MotionProjection::getId).distinct().toList();
    if (motionIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, OfficeElectionProposalProjection> proposalByMotion =
        proposals.findProjectionsByPolityIdAndMotionIdIn(polityId, motionIds).stream()
            .collect(
                Collectors.toMap(
                    OfficeElectionProposalProjection::getMotionId,
                    Function.identity(),
                    (first, ignored) -> first));
    if (proposalByMotion.isEmpty()) {
      return Map.of();
    }
    Set<UUID> officeIds =
        proposalByMotion.values().stream()
            .map(OfficeElectionProposalProjection::getOfficeId)
            .collect(Collectors.toSet());
    Map<UUID, OfficeProjection> officeById =
        offices.findProjectionsByPolityIdAndIdIn(polityId, officeIds).stream()
            .collect(Collectors.toMap(OfficeProjection::getId, Function.identity()));
    Set<String> officeCodes =
        officeById.values().stream().map(OfficeProjection::getCode).collect(Collectors.toSet());
    List<OfficeElectionCandidateProjection> candidateList =
        candidates.findProjectionsByPolityIdAndMotionIdIn(polityId, motionIds);
    Set<UUID> membershipIds =
        candidateList.stream()
            .map(OfficeElectionCandidateProjection::getMembershipId)
            .collect(Collectors.toSet());
    Map<UUID, MembershipProjection> memberById =
        membershipIds.isEmpty()
            ? Map.of()
            : memberships.findProjectionsByPolityIdAndIdIn(polityId, membershipIds).stream()
                .collect(Collectors.toMap(MembershipProjection::getId, Function.identity()));
    Map<UUID, List<OfficeElectionCandidateProjection>> candidatesByMotion =
        candidateList.stream()
            .collect(Collectors.groupingBy(OfficeElectionCandidateProjection::getMotionId));
    List<OfficeElectionBallotProjection> ballotList =
        ballots.findProjectionsByPolityIdAndMotionIdIn(polityId, motionIds);
    Map<UUID, List<OfficeElectionBallotPreferenceProjection>> preferencesByMotion =
        preferences
            .findProjectionsByPolityIdAndMotionIdInOrderByMembershipIdAscRankAsc(
                polityId, motionIds)
            .stream()
            .collect(Collectors.groupingBy(OfficeElectionBallotPreferenceProjection::getMotionId));
    List<OfficeTermProjection> activeTerms =
        officeCodes.isEmpty()
            ? List.of()
            : officeTerms.findProjectionsByPolityIdAndOfficeCodeInAndStatusAndEndsAtAfter(
                polityId, officeCodes, OfficeTermStatus.ACTIVE, now);
    Set<UUID> standingMembershipIds =
        membershipService.politicalStanding(polityId, membershipIds, now);
    Map<UUID, OfficeElectionBallotProjection> currentBallotByMotion =
        currentMembershipId == null
            ? Map.of()
            : ballotList.stream()
                .filter(ballot -> ballot.getMembershipId().equals(currentMembershipId))
                .collect(
                    Collectors.toMap(
                        OfficeElectionBallotProjection::getMotionId,
                        Function.identity(),
                        (first, ignored) -> first));
    return motions.stream()
        .filter(motion -> proposalByMotion.containsKey(motion.getId()))
        .collect(
            Collectors.toMap(
                MotionProjection::getId,
                motion ->
                    resolve(
                        motion,
                        required(
                            proposalByMotion,
                            motion.getId(),
                            "office_election_not_found",
                            "Office election not found."),
                        officeById,
                        candidatesByMotion.getOrDefault(motion.getId(), List.of()),
                        memberById,
                        currentBallotByMotion.get(motion.getId()),
                        preferencesByMotion.getOrDefault(motion.getId(), List.of()),
                        required(
                            procedures,
                            motion.getProcedureId(),
                            "procedure_not_found",
                            "Procedure not found."),
                        electorCounts.getOrDefault(motion.getId(), 0),
                        certifications.get(motion.getId()),
                        activeTerms,
                        standingMembershipIds,
                        now)));
  }

  private OfficeElectionResolution resolve(
      MotionProjection motion,
      OfficeElectionProposalProjection proposal,
      Map<UUID, OfficeProjection> officeById,
      List<OfficeElectionCandidateProjection> candidateList,
      Map<UUID, MembershipProjection> memberById,
      OfficeElectionBallotProjection currentBallot,
      List<OfficeElectionBallotPreferenceProjection> preferenceList,
      ProcedureProjection procedure,
      int eligible,
      CertificationProjection certification,
      List<OfficeTermProjection> activeTerms,
      Set<UUID> standingMembershipIds,
      OffsetDateTime now) {
    OfficeProjection office =
        required(officeById, proposal.getOfficeId(), "office_not_found", "Office not found.");
    List<OfficeElectionCandidateResult> candidateResults =
        candidateList.stream()
            .map(
                candidate -> {
                  MembershipProjection member =
                      required(
                          memberById,
                          candidate.getMembershipId(),
                          "member_not_found",
                          "Member not found.");
                  return mapper.toCandidateResult(
                      member.getId(),
                      member.getDisplayName(),
                      candidate.getStatus(),
                      candidate.getRespondedAt());
                })
            .sorted(
                Comparator.comparing(OfficeElectionCandidateResult::name)
                    .thenComparing(candidate -> candidate.membershipId().toString()))
            .toList();
    List<UUID> currentRanking =
        currentBallot == null
            ? List.of()
            : preferenceList.stream()
                .filter(
                    preference ->
                        preference.getMembershipId().equals(currentBallot.getMembershipId()))
                .sorted(Comparator.comparingInt(OfficeElectionBallotPreferenceProjection::getRank))
                .map(OfficeElectionBallotPreferenceProjection::getCandidateMembershipId)
                .toList();
    OfficeElectionBallotResult currentBallotResult =
        currentBallot == null
            ? null
            : new OfficeElectionBallotResult(currentBallot.getCastAt(), currentRanking);
    List<OfficeElectionBallotRanking> rankings = rankings(preferenceList);
    List<OfficeElectionCandidateOption> tallyCandidates =
        candidateList.stream()
            .filter(candidate -> candidate.getStatus() == OfficeElectionCandidateStatus.ACCEPTED)
            .filter(
                candidate ->
                    motion.getStatus() != MotionStatus.VOTING
                        || standingMembershipIds.contains(candidate.getMembershipId()))
            .filter(
                candidate ->
                    motion.getStatus() != MotionStatus.VOTING
                        || !holdsOffice(
                            candidate.getMembershipId(), office.getCode(), activeTerms, now))
            .map(
                candidate -> {
                  MembershipProjection member =
                      required(
                          memberById,
                          candidate.getMembershipId(),
                          "member_not_found",
                          "Member not found.");
                  return new OfficeElectionCandidateOption(member.getId(), member.getDisplayName());
                })
            .toList();
    var tally =
        certification != null && certification.getElectionTallySnapshot() != null
            ? certification.getElectionTallySnapshot()
            : evaluator.evaluate(
                procedure,
                eligible,
                proposal.getSeatsAvailable(),
                proposal.getMethod(),
                tallyCandidates,
                rankings);
    return new OfficeElectionResolution(
        mapper.toResult(
            office.getId(),
            office.getCode(),
            office.getName(),
            office.getNameKey(),
            proposal.getSeatsAvailable(),
            proposal.getMethod(),
            currentBallotResult,
            candidateResults),
        tally);
  }

  private List<OfficeElectionBallotRanking> rankings(
      List<OfficeElectionBallotPreferenceProjection> preferences) {
    Map<UUID, List<UUID>> rankings = new LinkedHashMap<>();
    preferences.stream()
        .sorted(
            Comparator.comparing(OfficeElectionBallotPreferenceProjection::getMembershipId)
                .thenComparingInt(OfficeElectionBallotPreferenceProjection::getRank))
        .forEach(
            preference ->
                rankings
                    .computeIfAbsent(
                        preference.getMembershipId(), ignored -> new java.util.ArrayList<>())
                    .add(preference.getCandidateMembershipId()));
    return rankings.entrySet().stream()
        .map(entry -> new OfficeElectionBallotRanking(entry.getKey(), entry.getValue()))
        .toList();
  }

  private boolean holdsOffice(
      UUID membershipId,
      String officeCode,
      Collection<OfficeTermProjection> activeTerms,
      OffsetDateTime now) {
    return activeTerms.stream()
        .anyMatch(
            term ->
                term.getMembershipId().equals(membershipId)
                    && term.getOfficeCode().equals(officeCode)
                    && term.getEndsAt().isAfter(now));
  }

  private <T> T required(Map<UUID, T> values, UUID id, String code, String message) {
    T value = values.get(id);
    if (value == null) {
      throw ApiException.notFound(code, message);
    }
    return value;
  }
}
