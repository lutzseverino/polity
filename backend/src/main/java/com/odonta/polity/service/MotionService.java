package com.odonta.polity.service;

import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.polity.api.model.CastVoteInput;
import com.odonta.polity.api.model.CreateMotionInput;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionDetails;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Resolution;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.VoteRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class MotionService {
  private final Clock clock;
  private final CertificationRepository certifications;
  private final ConstitutionalAuthority authority;
  private final ConstitutionVersionRepository constitutions;
  private final MotionElectorRepository electors;
  private final MembershipReader membershipReader;
  private final MembershipRepository memberships;
  private final MotionRepository motions;
  private final OfficialRecordWriter record;
  private final PolityService polities;
  private final ProcedureRepository procedures;
  private final ResolutionRepository resolutions;
  private final VoteRepository votes;
  private final VotingEvaluator voting;

  @Transactional
  public MotionDetails create(
      UUID polityId, AuthenticatedUser actor, @Valid CreateMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Membership introducer = membershipReader.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_MOTION);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId);
    Procedure procedure = procedure(constitution.getId());
    Motion motion =
        motions.saveAndFlush(
            new Motion(
                polityId,
                jurisdiction.getId(),
                institution.getId(),
                constitution.getId(),
                procedure.getId(),
                introducer.getId(),
                input.getTitle(),
                input.getBody(),
                EffectType.ADOPT_RESOLUTION,
                now));
    List<Membership> eligible =
        memberships.findByPolityIdAndStatusOrderByAdmittedAtAsc(polityId, MembershipStatus.ACTIVE);
    electors.saveAllAndFlush(
        eligible.stream()
            .map(member -> new MotionElector(polityId, motion.getId(), member.getId()))
            .toList());
    record.append(
        polityId,
        jurisdiction.getId(),
        constitution.getId(),
        introducer.getId(),
        OfficialRecordType.MOTION_INTRODUCED,
        motion.getId(),
        input.getTitle(),
        "%s introduced a resolution for the %s under Constitution v%d. Voting opened with %d eligible citizens."
            .formatted(
                introducer.getDisplayName(),
                institution.getName(),
                constitution.getVersion(),
                eligible.size()),
        now);
    return details(motion);
  }

  public List<MotionDetails> list(UUID polityId, UUID userId) {
    membershipReader.active(polityId, userId);
    return motions.findByPolityIdOrderByOpenedAtDesc(polityId).stream().map(this::details).toList();
  }

  public MotionDetails get(UUID polityId, UUID motionId, UUID userId) {
    membershipReader.active(polityId, userId);
    return details(motion(polityId, motionId));
  }

  @Transactional
  public MotionDetails vote(
      UUID polityId, UUID motionId, AuthenticatedUser actor, @Valid CastVoteInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    VoteChoice choice = input.getChoice();
    Membership voter = membershipReader.active(polityId, actor.id());
    Motion motion = motion(polityId, motionId);
    requireVoting(motion);
    if (!electors.existsByMotionIdAndMembershipId(motionId, voter.getId())) {
      throw ApiException.forbidden(
          "vote_ineligible", "This member was not eligible when voting opened.");
    }
    Vote vote =
        votes
            .findByMotionIdAndMembershipId(motionId, voter.getId())
            .map(
                existing -> {
                  existing.replace(choice, now);
                  return existing;
                })
            .orElseGet(() -> new Vote(polityId, motionId, voter.getId(), choice, now));
    votes.saveAndFlush(vote);
    record.append(
        polityId,
        motion.getJurisdictionId(),
        motion.getConstitutionVersionId(),
        voter.getId(),
        OfficialRecordType.VOTE_CAST,
        motionId,
        "Vote recorded on " + motion.getTitle(),
        voter.getDisplayName() + " cast or updated a ballot while voting was open.",
        now);
    return details(motion);
  }

  @Transactional
  public MotionDetails certify(UUID polityId, UUID motionId, AuthenticatedUser actor) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Membership requester = membershipReader.active(polityId, actor.id());
    Motion motion = motion(polityId, motionId);
    requireVoting(motion);
    ConstitutionVersion constitution =
        constitutions
            .findById(motion.getConstitutionVersionId())
            .orElseThrow(
                () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
    authority.require(requester, constitution, PowerCode.REQUEST_CERTIFICATION);
    Procedure procedure =
        procedures
            .findById(motion.getProcedureId())
            .orElseThrow(
                () -> ApiException.notFound("procedure_not_found", "Procedure not found."));
    VotingResult result =
        voting.evaluate(
            procedure,
            Math.toIntExact(electors.countByMotionId(motionId)),
            votes.findByMotionId(motionId));
    Certification certification =
        certifications.saveAndFlush(
            new Certification(polityId, motionId, requester.getId(), result, now));
    motion.certify(result.passed(), now);
    motions.saveAndFlush(motion);
    record.append(
        polityId,
        motion.getJurisdictionId(),
        constitution.getId(),
        requester.getId(),
        OfficialRecordType.MOTION_CERTIFIED,
        certification.getId(),
        "Result certified: " + motion.getTitle(),
        result.explanation(),
        now);
    if (result.passed()) {
      applyResolution(motion, requester, constitution, now);
    } else {
      record.append(
          polityId,
          motion.getJurisdictionId(),
          constitution.getId(),
          requester.getId(),
          OfficialRecordType.MOTION_REJECTED,
          motionId,
          "Motion rejected: " + motion.getTitle(),
          "The certified result did not authorize the proposed effect.",
          now);
    }
    return details(motion);
  }

  private void applyResolution(
      Motion motion, Membership requester, ConstitutionVersion constitution, OffsetDateTime now) {
    if (motion.getEffectType() != EffectType.ADOPT_RESOLUTION) {
      throw new IllegalStateException("Unsupported official effect: " + motion.getEffectType());
    }
    Resolution resolution =
        resolutions.saveAndFlush(
            new Resolution(
                motion.getPolityId(), motion.getId(), motion.getTitle(), motion.getBody(), now));
    record.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        requester.getId(),
        OfficialRecordType.RESOLUTION_ADOPTED,
        resolution.getId(),
        "Resolution adopted: " + motion.getTitle(),
        motion.getBody(),
        now);
  }

  private MotionDetails details(Motion motion) {
    Procedure procedure =
        procedures
            .findById(motion.getProcedureId())
            .orElseThrow(
                () -> ApiException.notFound("procedure_not_found", "Procedure not found."));
    ConstitutionVersion constitution =
        constitutions
            .findById(motion.getConstitutionVersionId())
            .orElseThrow(
                () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
    Membership introducer =
        memberships
            .findById(motion.getIntroducedBy())
            .orElseThrow(() -> ApiException.notFound("member_not_found", "Member not found."));
    VotingResult tally =
        voting.evaluate(
            procedure,
            Math.toIntExact(electors.countByMotionId(motion.getId())),
            votes.findByMotionId(motion.getId()));
    return new MotionDetails(
        motion,
        procedure,
        constitution,
        introducer,
        tally,
        certifications.findByMotionId(motion.getId()).orElse(null));
  }

  private Motion motion(UUID polityId, UUID motionId) {
    return motions
        .findByIdAndPolityId(motionId, polityId)
        .orElseThrow(() -> ApiException.notFound("motion_not_found", "Motion not found."));
  }

  private Procedure procedure(UUID constitutionId) {
    return procedures
        .findByConstitutionVersionIdAndCode(constitutionId, Procedure.ORDINARY_RESOLUTION)
        .orElseThrow(() -> ApiException.notFound("procedure_not_found", "Procedure not found."));
  }

  private void requireVoting(Motion motion) {
    if (motion.getStatus() != MotionStatus.VOTING) {
      throw ApiException.conflict("motion_not_voting", "This motion is no longer open for voting.");
    }
  }
}
