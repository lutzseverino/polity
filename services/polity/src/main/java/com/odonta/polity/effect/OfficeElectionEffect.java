package com.odonta.polity.effect;

import com.odonta.common.api.ApiException;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallotRanking;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.OfficeElectionBallotPreferenceRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.service.MembershipService;
import com.odonta.polity.service.OfficialRecordService;
import java.time.OffsetDateTime;
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
final class OfficeElectionEffect implements MotionEffect {
  private final MotionElectorRepository electors;
  private final OfficeElectionBallotPreferenceRepository ballotPreferences;
  private final OfficeElectionCandidateRepository candidates;
  private final OfficeElectionEvaluator elections;
  private final OfficeElectionProposalRepository proposals;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final ProcedureRepository procedures;
  private final MembershipRepository memberships;
  private final MembershipService membershipService;
  private final OfficialRecordService officialRecords;

  @Override
  public EffectType type() {
    return EffectType.ELECT_OFFICE;
  }

  @Override
  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    var proposal =
        proposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "office_election_proposal_not_found",
                        "Office election proposal not found."));
    Office office =
        offices
            .findEntityByIdAndPolityId(proposal.getOfficeId(), motion.getPolityId())
            .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
    Procedure procedure =
        procedures
            .findEntityById(motion.getProcedureId())
            .orElseThrow(
                () -> ApiException.notFound("procedure_not_found", "Procedure not found."));
    Set<UUID> currentHolderIds = currentHolderIds(motion.getPolityId(), office, now);
    List<OfficeElectionCandidateOption> candidateOptions =
        electionCandidates(motion, currentHolderIds, now);
    OfficeElectionTallyResult result =
        elections.evaluate(
            procedure,
            Math.toIntExact(electors.countByMotionId(motion.getId())),
            proposal.getSeatsAvailable(),
            proposal.getMethod(),
            candidateOptions,
            ballotRankings(motion.getId()));
    if (!result.passed() || result.winners().isEmpty()) {
      throw ApiException.conflict(
          "office_election_not_passed", "Only passed office elections can assign office terms.");
    }
    int vacantSeats = vacantSeatCount(office, now);
    if (vacantSeats < result.seatsFilled()) {
      throw ApiException.conflict(
          "office_seats_full", "This office has no vacant seats for another active term.");
    }
    for (var winner : result.winners()) {
      if (currentHolderIds.contains(winner.membershipId())) {
        throw ApiException.conflict(
            "office_term_already_held",
            "The elected member already holds an active term for this office.");
      }
      OfficeTerm term =
          officeTerms.saveAndFlush(
              new OfficeTerm(
                  motion.getPolityId(),
                  office.getId(),
                  office.getCode(),
                  winner.membershipId(),
                  motion.getId(),
                  now,
                  now.plusDays(office.getTermLengthDays())));
      officialRecords.append(
          motion.getPolityId(),
          motion.getJurisdictionId(),
          constitution.getId(),
          actor.getId(),
          OfficialRecordType.OFFICE_ELECTED,
          term.getId(),
          OfficialRecordContext.effect(motion, OfficialRecordOutcome.OFFICE_ELECTED),
          OfficialRecordTemplate.of(
              OfficialRecordTemplateKey.OFFICE_ELECTED,
              TemplateParameters.of(
                  "memberName",
                  winner.name(),
                  "officeName",
                  office.getName(),
                  "officeNameKey",
                  office.getNameKey(),
                  "officeCode",
                  office.getCode(),
                  "termLengthDays",
                  office.getTermLengthDays())),
          now);
    }
  }

  private int vacantSeatCount(Office office, OffsetDateTime now) {
    long activeTerms =
        officeTerms.countByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            office.getPolityId(), office.getCode(), OfficeTermStatus.ACTIVE, now);
    return Math.max(0, office.getSeatCount() - Math.toIntExact(activeTerms));
  }

  private List<OfficeElectionCandidateOption> electionCandidates(
      Motion motion, Set<UUID> currentHolderIds, OffsetDateTime now) {
    var acceptedCandidates =
        candidates.findEntitiesByMotionIdAndStatus(
            motion.getId(), OfficeElectionCandidateStatus.ACCEPTED);
    if (acceptedCandidates.isEmpty()) {
      return List.of();
    }
    Set<UUID> candidateMembershipIds =
        acceptedCandidates.stream()
            .map(candidate -> candidate.getMembershipId())
            .collect(Collectors.toSet());
    Map<UUID, MembershipProjection> membersById =
        memberships
            .findProjectionsByPolityIdAndIdIn(motion.getPolityId(), candidateMembershipIds)
            .stream()
            .collect(Collectors.toMap(MembershipProjection::getId, Function.identity()));
    Set<UUID> standingMembershipIds =
        membershipService.politicalStanding(motion.getPolityId(), candidateMembershipIds, now);
    return acceptedCandidates.stream()
        .map(
            candidate -> {
              MembershipProjection membership = membersById.get(candidate.getMembershipId());
              if (membership == null) {
                throw ApiException.notFound("member_not_found", "Member not found.");
              }
              if (!standingMembershipIds.contains(membership.getId())) {
                return null;
              }
              if (currentHolderIds.contains(membership.getId())) {
                return null;
              }
              return new OfficeElectionCandidateOption(
                  membership.getId(), membership.getDisplayName());
            })
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private Set<UUID> currentHolderIds(UUID polityId, Office office, OffsetDateTime now) {
    return officeTerms
        .findEntitiesByPolityIdAndOfficeCodeAndStatusAndEndsAtAfterOrderByStartedAtAsc(
            polityId, office.getCode(), OfficeTermStatus.ACTIVE, now)
        .stream()
        .map(term -> term.getMembershipId())
        .collect(Collectors.toSet());
  }

  private List<OfficeElectionBallotRanking> ballotRankings(UUID motionId) {
    Map<UUID, List<UUID>> candidateMembershipIdsByMembershipId = new LinkedHashMap<>();
    ballotPreferences
        .findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(motionId)
        .forEach(
            preference ->
                candidateMembershipIdsByMembershipId
                    .computeIfAbsent(
                        preference.getMembershipId(), ignored -> new java.util.ArrayList<>())
                    .add(preference.getCandidateMembershipId()));
    return candidateMembershipIdsByMembershipId.entrySet().stream()
        .map(entry -> new OfficeElectionBallotRanking(entry.getKey(), entry.getValue()))
        .toList();
  }
}
