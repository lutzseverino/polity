package com.odonta.polity.resolver;

import static com.odonta.polity.exception.RequiredResource.required;

import com.odonta.polity.evaluator.VotingEvaluator;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.mapper.CertificationApplicationMapper;
import com.odonta.polity.mapper.MotionApplicationMapper;
import com.odonta.polity.model.CertificationModality;
import com.odonta.polity.model.CertificationOutcomeReason;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingOutcomeReason;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.repository.CertificationProjection;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionVersionProjection;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorProjection;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.PolityProjection;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureProjection;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.VoteProjection;
import com.odonta.polity.repository.VoteRepository;
import com.odonta.polity.result.CertificationResult;
import com.odonta.polity.result.ConstitutionAmendmentProposalResult;
import com.odonta.polity.result.MotionResult;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
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
public class MotionResultResolver {
  private final Clock clock;
  private final MotionApplicationMapper mapper;
  private final CertificationApplicationMapper certificationMapper;
  private final MotionRepository motions;
  private final PolityRepository polities;
  private final ConstitutionVersionRepository constitutions;
  private final ProcedureRepository procedures;
  private final MembershipRepository memberships;
  private final CertificationRepository certifications;
  private final MotionElectorRepository electors;
  private final VoteRepository votes;
  private final VotingEvaluator voting;
  private final OfficeElectionResultResolver officeElections;
  private final ConstitutionAmendmentResultResolver amendments;
  private final MotionActionAvailabilityResolver actions;

  public MotionResult resolve(UUID polityId, UUID motionId, UUID currentMembershipId) {
    MotionProjection motion =
        motions
            .findProjectedByIdAndPolityId(motionId, polityId)
            .orElseThrow(PolityResource.MOTION::notFound);
    return resolveAll(polityId, List.of(motion), currentMembershipId).getFirst();
  }

  public MotionResult resolve(Motion motion, UUID currentMembershipId) {
    return resolve(motion.getPolityId(), motion.getId(), currentMembershipId);
  }

  public List<MotionResult> resolveAll(
      UUID polityId, List<MotionProjection> motionList, UUID currentMembershipId) {
    if (motionList.isEmpty()) {
      return List.of();
    }
    PolityProjection polity =
        polities.findProjectedById(polityId).orElseThrow(PolityResource.POLITY::notFound);
    List<UUID> motionIds = motionList.stream().map(MotionProjection::getId).distinct().toList();
    Set<UUID> constitutionIds =
        motionList.stream()
            .map(MotionProjection::getConstitutionVersionId)
            .collect(Collectors.toSet());
    Set<UUID> procedureIds =
        motionList.stream().map(MotionProjection::getProcedureId).collect(Collectors.toSet());
    Set<UUID> membershipIds =
        new HashSet<>(
            motionList.stream().map(MotionProjection::getIntroducedBy).collect(Collectors.toSet()));
    if (currentMembershipId != null) {
      membershipIds.add(currentMembershipId);
    }
    Map<UUID, ConstitutionVersionProjection> constitutionById =
        constitutions.findProjectionsByPolityIdAndIdIn(polityId, constitutionIds).stream()
            .collect(Collectors.toMap(ConstitutionVersionProjection::getId, Function.identity()));
    Map<UUID, ProcedureProjection> procedureById =
        procedures.findProjectionsByPolityIdAndIdIn(polityId, procedureIds).stream()
            .collect(Collectors.toMap(ProcedureProjection::getId, Function.identity()));
    Map<UUID, MembershipProjection> memberById =
        memberships.findProjectionsByPolityIdAndIdIn(polityId, membershipIds).stream()
            .collect(Collectors.toMap(MembershipProjection::getId, Function.identity()));
    Map<UUID, CertificationProjection> certificationByMotion =
        certifications.findProjectionsByPolityIdAndMotionIdIn(polityId, motionIds).stream()
            .collect(
                Collectors.toMap(
                    CertificationProjection::getMotionId,
                    Function.identity(),
                    (first, ignored) -> first));
    List<MotionElectorProjection> electorList =
        electors.findProjectionsByPolityIdAndMotionIdIn(polityId, motionIds);
    Map<UUID, Integer> electorCountByMotion =
        electorList.stream()
            .collect(
                Collectors.groupingBy(
                    MotionElectorProjection::getMotionId,
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
    List<VoteProjection> voteList =
        votes.findProjectionsByPolityIdAndMotionIdIn(polityId, motionIds);
    Map<UUID, List<VoteProjection>> votesByMotion =
        voteList.stream().collect(Collectors.groupingBy(VoteProjection::getMotionId));
    OffsetDateTime now = OffsetDateTime.now(clock);
    List<MotionProjection> electionMotions =
        motionList.stream()
            .filter(motion -> motion.getEffectType() == EffectType.ELECT_OFFICE)
            .toList();
    Map<UUID, OfficeElectionResolution> electionByMotion =
        officeElections.resolveAll(
            polityId,
            electionMotions,
            currentMembershipId,
            procedureById,
            electorCountByMotion,
            certificationByMotion,
            now);
    List<UUID> amendmentMotionIds =
        motionList.stream()
            .filter(motion -> motion.getEffectType() == EffectType.AMEND_CONSTITUTION)
            .map(MotionProjection::getId)
            .toList();
    Map<UUID, ConstitutionAmendmentProposalResult> amendmentByMotion =
        amendments.resolveAll(polityId, amendmentMotionIds);
    MembershipProjection currentMember =
        currentMembershipId == null
            ? null
            : required(memberById, currentMembershipId, PolityResource.MEMBER);
    Set<UUID> eligibleMotionIds =
        currentMembershipId == null
            ? Set.of()
            : electorList.stream()
                .filter(elector -> elector.getMembershipId().equals(currentMembershipId))
                .map(MotionElectorProjection::getMotionId)
                .collect(Collectors.toSet());
    var actionsByMotion =
        actions.resolveAll(
            polityId,
            motionList,
            constitutionById,
            polity.getStatus(),
            currentMember,
            eligibleMotionIds);
    return motionList.stream()
        .map(
            motion -> {
              ProcedureProjection procedure =
                  required(procedureById, motion.getProcedureId(), PolityResource.PROCEDURE);
              ConstitutionVersionProjection constitution =
                  required(
                      constitutionById,
                      motion.getConstitutionVersionId(),
                      PolityResource.CONSTITUTION);
              MembershipProjection introducer =
                  required(memberById, motion.getIntroducedBy(), PolityResource.MEMBER);
              List<VoteProjection> motionVotes =
                  votesByMotion.getOrDefault(motion.getId(), List.of());
              CertificationProjection certification = certificationByMotion.get(motion.getId());
              CertificationResult certificationResult =
                  certification == null ? null : certificationMapper.toResult(certification);
              VotingResult tally =
                  motion.getEffectType() == EffectType.ELECT_OFFICE
                      ? null
                      : certification == null
                          ? voting.evaluateChoices(
                              procedure,
                              electorCountByMotion.getOrDefault(motion.getId(), 0),
                              motionVotes.stream().map(VoteProjection::getChoice).toList())
                          : certifiedVotingResult(certification);
              OfficeElectionResolution election = electionByMotion.get(motion.getId());
              return mapper.toResult(
                  motion,
                  constitution.getVersion(),
                  procedure.getName(),
                  procedure.getNameKey(),
                  introducer.getDisplayName(),
                  tally,
                  election == null ? null : election.result(),
                  election == null ? null : election.tally(),
                  certificationResult,
                  currentVote(motionVotes, currentMembershipId),
                  actionsByMotion.get(motion.getId()),
                  amendmentByMotion.get(motion.getId()));
            })
        .toList();
  }

  private VotingResult certifiedVotingResult(CertificationProjection certification) {
    if (certification.getModality() != CertificationModality.YES_NO
        || certification.getYesCount() == null
        || certification.getNoCount() == null
        || certification.getAbstainCount() == null) {
      throw new IllegalStateException("Yes/no certification evidence is incomplete.");
    }
    return new VotingResult(
        certification.getEligibleCount(),
        certification.getYesCount(),
        certification.getNoCount(),
        certification.getAbstainCount(),
        certification.getQuorumRequired(),
        certification.isQuorumMet(),
        certification.isThresholdMet(),
        certification.isPassed(),
        votingOutcomeReason(certification.getOutcomeReason()));
  }

  private VotingOutcomeReason votingOutcomeReason(CertificationOutcomeReason reason) {
    return switch (reason) {
      case PASSED -> VotingOutcomeReason.PASSED;
      case QUORUM_NOT_MET -> VotingOutcomeReason.QUORUM_NOT_MET;
      case THRESHOLD_NOT_MET -> VotingOutcomeReason.THRESHOLD_NOT_MET;
      case NO_DECISIVE_RESULT ->
          throw new IllegalStateException("Yes/no certification cannot have an election outcome.");
    };
  }

  private VoteChoice currentVote(List<VoteProjection> votes, UUID currentMembershipId) {
    if (currentMembershipId == null) {
      return null;
    }
    return votes.stream()
        .filter(vote -> vote.getMembershipId().equals(currentMembershipId))
        .map(VoteProjection::getChoice)
        .findFirst()
        .orElse(null);
  }
}
