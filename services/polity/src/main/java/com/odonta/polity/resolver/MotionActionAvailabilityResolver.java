package com.odonta.polity.resolver;

import static com.odonta.polity.exception.RequiredResource.required;

import com.odonta.common.api.ApiException;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionProjection;
import com.odonta.polity.repository.ConstitutionalPowerProjection;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.OfficeElectionCandidateProjection;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeTermProjection;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.result.ActionAvailabilityResult;
import com.odonta.polity.result.ActionUnavailableReason;
import com.odonta.polity.result.MotionActionAvailabilityResult;
import com.odonta.polity.service.MembershipService;
import java.time.Clock;
import java.time.OffsetDateTime;
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
public class MotionActionAvailabilityResolver {
  private final Clock clock;
  private final AppealProposalRepository appealProposals;
  private final ConstitutionalAuthority authority;
  private final ConstitutionalPowerRepository powers;
  private final MembershipService membershipService;
  private final OfficeElectionCandidateRepository electionCandidates;
  private final OfficeTermRepository officeTerms;

  public Map<UUID, MotionActionAvailabilityResult> resolveAll(
      UUID polityId,
      List<MotionProjection> motions,
      Map<UUID, ConstitutionVersionProjection> constitutions,
      PolityStatus polityStatus,
      MembershipProjection currentMember,
      Set<UUID> eligibleMotionIds) {
    if (motions.isEmpty()) {
      return Map.of();
    }
    if (polityStatus == PolityStatus.DISBANDED) {
      return unavailable(motions, ActionUnavailableReason.POLITY_DISBANDED);
    }
    if (currentMember == null) {
      return unavailable(motions, ActionUnavailableReason.POLITY_MEMBERSHIP_REQUIRED);
    }
    UUID currentMembershipId = currentMember.getId();
    List<UUID> motionIds = motions.stream().map(MotionProjection::getId).distinct().toList();
    Set<UUID> constitutionIds =
        motions.stream()
            .map(MotionProjection::getConstitutionVersionId)
            .collect(Collectors.toSet());
    Set<UUID> candidacyMotionIds =
        electionCandidates.findProjectionsByPolityIdAndMotionIdIn(polityId, motionIds).stream()
            .filter(candidate -> candidate.getMembershipId().equals(currentMembershipId))
            .map(OfficeElectionCandidateProjection::getMotionId)
            .collect(Collectors.toSet());
    Map<UUID, AppealProposalProjection> appealByMotion =
        appealProposals.findProjectionsByPolityIdAndMotionIdIn(polityId, motionIds).stream()
            .collect(
                Collectors.toMap(
                    AppealProposalProjection::getMotionId,
                    Function.identity(),
                    (first, ignored) -> first));
    Map<UUID, ConstitutionalPowerProjection> certificationPowerByConstitution =
        powers
            .findProjectionsByPolityIdAndConstitutionVersionIdInAndCode(
                polityId, constitutionIds, PowerCode.REQUEST_CERTIFICATION)
            .stream()
            .collect(
                Collectors.toMap(
                    ConstitutionalPowerProjection::getConstitutionVersionId,
                    Function.identity(),
                    (first, ignored) -> first));
    OffsetDateTime now = OffsetDateTime.now(clock);
    List<OfficeTermProjection> heldTerms =
        officeTerms.findProjectionsByPolityIdAndMembershipIdAndStatusAndEndsAtAfter(
            polityId, currentMembershipId, com.odonta.polity.model.OfficeTermStatus.ACTIVE, now);
    boolean standing =
        membershipService
            .politicalStanding(polityId, List.of(currentMembershipId), now)
            .contains(currentMembershipId);
    return motions.stream()
        .collect(
            Collectors.toMap(
                MotionProjection::getId,
                motion ->
                    resolve(
                        motion,
                        required(
                            constitutions,
                            motion.getConstitutionVersionId(),
                            PolityResource.CONSTITUTION),
                        currentMember,
                        eligibleMotionIds,
                        candidacyMotionIds,
                        appealByMotion,
                        certificationPowerByConstitution,
                        heldTerms,
                        standing,
                        now)));
  }

  private Map<UUID, MotionActionAvailabilityResult> unavailable(
      List<MotionProjection> motions, ActionUnavailableReason reason) {
    ActionAvailabilityResult unavailable = ActionAvailabilityResult.blocked(reason);
    MotionActionAvailabilityResult result =
        new MotionActionAvailabilityResult(unavailable, unavailable, unavailable, unavailable);
    return motions.stream().collect(Collectors.toMap(MotionProjection::getId, ignored -> result));
  }

  private MotionActionAvailabilityResult resolve(
      MotionProjection motion,
      ConstitutionVersionProjection constitution,
      MembershipProjection member,
      Set<UUID> eligibleMotionIds,
      Set<UUID> candidacyMotionIds,
      Map<UUID, AppealProposalProjection> appealByMotion,
      Map<UUID, ConstitutionalPowerProjection> certificationPowerByConstitution,
      List<OfficeTermProjection> heldTerms,
      boolean standing,
      OffsetDateTime now) {
    return new MotionActionAvailabilityResult(
        voteAvailability(motion, eligibleMotionIds, now),
        electionBallotAvailability(motion, eligibleMotionIds, now),
        candidacyResponseAvailability(motion, candidacyMotionIds, now),
        certificationAvailability(
            motion,
            constitution,
            member,
            appealByMotion,
            certificationPowerByConstitution,
            heldTerms,
            standing,
            now));
  }

  private ActionAvailabilityResult voteAvailability(
      MotionProjection motion, Set<UUID> eligibleMotionIds, OffsetDateTime now) {
    if (motion.getEffectType() == EffectType.ELECT_OFFICE) {
      return ActionAvailabilityResult.blocked(
          ActionUnavailableReason.OFFICE_ELECTION_BALLOT_REQUIRED);
    }
    ActionAvailabilityResult window = votingWindowAvailability(motion, now);
    return window.available() ? electorAvailability(motion, eligibleMotionIds) : window;
  }

  private ActionAvailabilityResult electionBallotAvailability(
      MotionProjection motion, Set<UUID> eligibleMotionIds, OffsetDateTime now) {
    if (motion.getEffectType() != EffectType.ELECT_OFFICE) {
      return ActionAvailabilityResult.blocked(ActionUnavailableReason.MOTION_NOT_OFFICE_ELECTION);
    }
    ActionAvailabilityResult window = votingWindowAvailability(motion, now);
    return window.available() ? electorAvailability(motion, eligibleMotionIds) : window;
  }

  private ActionAvailabilityResult electorAvailability(
      MotionProjection motion, Set<UUID> eligibleMotionIds) {
    return eligibleMotionIds.contains(motion.getId())
        ? ActionAvailabilityResult.allowed()
        : ActionAvailabilityResult.blocked(ActionUnavailableReason.VOTE_INELIGIBLE);
  }

  private ActionAvailabilityResult votingWindowAvailability(
      MotionProjection motion, OffsetDateTime now) {
    if (motion.getStatus() != MotionStatus.VOTING) {
      return ActionAvailabilityResult.blocked(ActionUnavailableReason.MOTION_NOT_VOTING);
    }
    if (now.isBefore(motion.getVotingOpensAt())) {
      return ActionAvailabilityResult.blocked(ActionUnavailableReason.VOTING_NOT_OPEN);
    }
    return now.isBefore(motion.getVotingClosesAt())
        ? ActionAvailabilityResult.allowed()
        : ActionAvailabilityResult.blocked(ActionUnavailableReason.VOTING_CLOSED);
  }

  private ActionAvailabilityResult candidacyResponseAvailability(
      MotionProjection motion, Set<UUID> candidacyMotionIds, OffsetDateTime now) {
    if (motion.getEffectType() != EffectType.ELECT_OFFICE) {
      return ActionAvailabilityResult.blocked(ActionUnavailableReason.MOTION_NOT_OFFICE_ELECTION);
    }
    if (motion.getStatus() != MotionStatus.VOTING) {
      return ActionAvailabilityResult.blocked(ActionUnavailableReason.MOTION_NOT_VOTING);
    }
    if (!now.isBefore(motion.getVotingOpensAt())) {
      return ActionAvailabilityResult.blocked(ActionUnavailableReason.CANDIDACY_RESPONSE_CLOSED);
    }
    return candidacyMotionIds.contains(motion.getId())
        ? ActionAvailabilityResult.allowed()
        : ActionAvailabilityResult.blocked(ActionUnavailableReason.CANDIDACY_NOT_FOUND);
  }

  private ActionAvailabilityResult certificationAvailability(
      MotionProjection motion,
      ConstitutionVersionProjection constitution,
      MembershipProjection member,
      Map<UUID, AppealProposalProjection> appealByMotion,
      Map<UUID, ConstitutionalPowerProjection> certificationPowerByConstitution,
      List<OfficeTermProjection> heldTerms,
      boolean standing,
      OffsetDateTime now) {
    if (motion.getStatus() != MotionStatus.VOTING) {
      return ActionAvailabilityResult.blocked(ActionUnavailableReason.MOTION_NOT_VOTING);
    }
    if (now.isBefore(motion.getCertificationOpensAt())) {
      return ActionAvailabilityResult.blocked(ActionUnavailableReason.CERTIFICATION_NOT_OPEN);
    }
    if (constitution.getStatus() != ConstitutionStatus.RATIFIED) {
      return ActionAvailabilityResult.blocked(ActionUnavailableReason.CONSTITUTION_SUPERSEDED);
    }
    boolean ownAppeal =
        motion.getEffectType() == EffectType.GRANT_APPEAL
            && appealByMotion.containsKey(motion.getId())
            && appealByMotion.get(motion.getId()).getAppellantMembershipId().equals(member.getId());
    ConstitutionalPowerProjection power =
        certificationPowerByConstitution.get(motion.getConstitutionVersionId());
    try {
      return authority.allows(member, power, heldTerms, standing, !ownAppeal, now)
          ? ActionAvailabilityResult.allowed()
          : ActionAvailabilityResult.blocked(
              ActionUnavailableReason.CONSTITUTIONAL_AUTHORITY_MISSING);
    } catch (ApiException exception) {
      return ActionAvailabilityResult.blocked(
          ActionUnavailableReason.fromWireValue(exception.code()));
    }
  }
}
