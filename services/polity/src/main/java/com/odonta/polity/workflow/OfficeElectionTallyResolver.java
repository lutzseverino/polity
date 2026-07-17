package com.odonta.polity.workflow;

import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallotRanking;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.repository.OfficeElectionBallotPreferenceRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalProjection;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.resolver.ActiveMembershipResolver;
import com.odonta.polity.service.MembershipService;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OfficeElectionTallyResolver {
  private final ActiveMembershipResolver activeMemberships;
  private final OfficeElectionBallotPreferenceRepository ballotPreferences;
  private final OfficeElectionCandidateRepository candidates;
  private final OfficeElectionEvaluator evaluator;
  private final MembershipService memberships;
  private final OfficeElectionProposalRepository proposals;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;

  OfficeElectionTallyResult resolve(
      Procedure procedure, int eligible, UUID motionId, OffsetDateTime now) {
    return evaluator.evaluate(
        procedure,
        eligible,
        seatsAvailable(motionId),
        method(motionId),
        candidateOptions(motionId, now),
        ballotRankings(motionId));
  }

  private List<OfficeElectionCandidateOption> candidateOptions(UUID motionId, OffsetDateTime now) {
    return candidates
        .findEntitiesByMotionIdAndStatus(motionId, OfficeElectionCandidateStatus.ACCEPTED)
        .stream()
        .map(
            candidate ->
                candidateOption(
                    candidate.getPolityId(), candidate.getMembershipId(), motionId, now))
        .filter(Objects::nonNull)
        .toList();
  }

  private OfficeElectionCandidateOption candidateOption(
      UUID polityId, UUID membershipId, UUID motionId, OffsetDateTime now) {
    Membership membership = activeMemberships.resolveById(polityId, membershipId);
    if (!memberships.hasPoliticalStanding(membership.getId(), now)
        || officeHeldByCandidate(motionId, membership.getId(), now)) {
      return null;
    }
    return new OfficeElectionCandidateOption(membership.getId(), membership.getDisplayName());
  }

  private boolean officeHeldByCandidate(UUID motionId, UUID membershipId, OffsetDateTime now) {
    return proposals
        .findProjectedByMotionId(motionId)
        .map(
            proposal -> {
              Office office =
                  offices
                      .findEntityByIdAndPolityId(proposal.getOfficeId(), proposal.getPolityId())
                      .orElseThrow(PolityResource.OFFICE::notFound);
              return officeTerms
                  .existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndEndsAtAfter(
                      proposal.getPolityId(),
                      office.getCode(),
                      membershipId,
                      OfficeTermStatus.ACTIVE,
                      now);
            })
        .orElse(false);
  }

  private int seatsAvailable(UUID motionId) {
    return proposals
        .findProjectedByMotionId(motionId)
        .map(OfficeElectionProposalProjection::getSeatsAvailable)
        .orElse(0);
  }

  private OfficeElectionMethod method(UUID motionId) {
    return proposals
        .findProjectedByMotionId(motionId)
        .map(OfficeElectionProposalProjection::getMethod)
        .orElse(OfficeElectionMethod.RANKED_CHOICE);
  }

  private List<OfficeElectionBallotRanking> ballotRankings(UUID motionId) {
    Map<UUID, List<UUID>> candidateIdsByMember = new LinkedHashMap<>();
    ballotPreferences
        .findEntitiesByMotionIdOrderByMembershipIdAscRankAsc(motionId)
        .forEach(
            preference ->
                candidateIdsByMember
                    .computeIfAbsent(
                        preference.getMembershipId(), ignored -> new java.util.ArrayList<>())
                    .add(preference.getCandidateMembershipId()));
    return candidateIdsByMember.entrySet().stream()
        .map(entry -> new OfficeElectionBallotRanking(entry.getKey(), entry.getValue()))
        .toList();
  }
}
