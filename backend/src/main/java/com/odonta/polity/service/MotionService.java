package com.odonta.polity.service;

import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.mapper.MotionApplicationMapper;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.CastVoteInput;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.CreateAppealMotionInput;
import com.odonta.polity.model.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.model.CreateMotionInput;
import com.odonta.polity.model.CreateOfficeAssignmentMotionInput;
import com.odonta.polity.model.CreateProcedureChangeInput;
import com.odonta.polity.model.CreateSanctionMotionInput;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.MotionResult;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeAssignmentProposal;
import com.odonta.polity.model.OfficialRecordCitation;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionProposal;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeAssignmentProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.repository.VoteRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class MotionService {
  private final Clock clock;
  private final PolityAccessPolicy access;
  private final AppealRepository appeals;
  private final AppealProposalRepository appealProposals;
  private final CertificationRepository certifications;
  private final ConstitutionalAuthority authority;
  private final ConstitutionAmendmentProposalRepository amendmentProposals;
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals;
  private final ConstitutionVersionRepository constitutions;
  private final EffectApplicationService effects;
  private final MotionElectorRepository electors;
  private final MembershipReader membershipReader;
  private final MemberStandingService standing;
  private final MembershipRepository memberships;
  private final MotionApplicationMapper mapper;
  private final MotionRepository motions;
  private final OfficeAssignmentProposalRepository officeAssignmentProposals;
  private final OfficeRepository offices;
  private final OfficialRecordWriter record;
  private final PolityService polities;
  private final ProcedureRepository procedures;
  private final SanctionProposalRepository sanctionProposals;
  private final SanctionRepository sanctions;
  private final VoteRepository votes;
  private final VotingEvaluator voting;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult create(
      UUID polityId, AuthenticatedUser actor, @Valid CreateMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Membership introducer = membershipReader.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_MOTION);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.ORDINARY_RESOLUTION);
    Motion motion =
        createMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            input.title(),
            input.body(),
            PowerCode.INTRODUCE_MOTION,
            now);
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult createOfficeAssignment(
      UUID polityId, AuthenticatedUser actor, @Valid CreateOfficeAssignmentMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Membership introducer = membershipReader.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_OFFICE_ASSIGNMENT);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.OFFICE_ASSIGNMENT);
    Office office =
        offices
            .findByIdAndPolityId(input.officeId(), polityId)
            .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
    if (!office.getConstitutionVersionId().equals(constitution.getId())) {
      throw ApiException.conflict(
          "office_not_current", "This office belongs to a previous constitution version.");
    }
    Membership nominee = activeMembership(input.nomineeMembershipId(), polityId);
    Motion motion =
        createMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            "Assign " + nominee.getDisplayName() + " as " + office.getName(),
            "%s is nominated to serve as %s.".formatted(nominee.getDisplayName(), office.getName()),
            PowerCode.INTRODUCE_OFFICE_ASSIGNMENT,
            now);
    officeAssignmentProposals.saveAndFlush(
        new OfficeAssignmentProposal(polityId, motion.getId(), office.getId(), nominee.getId()));
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult createSanction(
      UUID polityId, AuthenticatedUser actor, @Valid CreateSanctionMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Membership introducer = membershipReader.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_SANCTION);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.SANCTION);
    Membership target = activeMembership(input.targetMembershipId(), polityId);
    Motion motion =
        createMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            "Sanction " + target.getDisplayName(),
            input.reason(),
            PowerCode.INTRODUCE_SANCTION,
            now);
    sanctionProposals.saveAndFlush(
        new SanctionProposal(
            polityId,
            motion.getId(),
            target.getId(),
            input.type(),
            input.reason(),
            input.durationDays()));
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult createAppeal(
      UUID polityId, AuthenticatedUser actor, @Valid CreateAppealMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Membership introducer = membershipReader.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_APPEAL);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.APPEAL);
    Sanction sanction =
        sanctions
            .findByIdAndPolityId(input.sanctionId(), polityId)
            .orElseThrow(() -> ApiException.notFound("sanction_not_found", "Sanction not found."));
    requireAppealable(polityId, sanction, now);
    Motion motion =
        createMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            "Appeal sanction",
            input.reason(),
            PowerCode.INTRODUCE_APPEAL,
            now);
    appealProposals.saveAndFlush(
        new AppealProposal(
            polityId,
            motion.getId(),
            sanction.getId(),
            sanction.getTargetMembershipId(),
            input.reason()));
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult createAmendment(
      UUID polityId, AuthenticatedUser actor, @Valid CreateConstitutionAmendmentMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Membership introducer = membershipReader.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_AMENDMENT);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.CONSTITUTION_AMENDMENT);
    List<CreateProcedureChangeInput> procedureChanges =
        validateProcedureChanges(input, constitution);
    Motion motion =
        createMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            "Amend constitution: " + input.title(),
            input.body(),
            PowerCode.INTRODUCE_AMENDMENT,
            now);
    ConstitutionAmendmentProposal proposal =
        amendmentProposals.saveAndFlush(
            new ConstitutionAmendmentProposal(
                polityId,
                motion.getId(),
                input.title(),
                input.body(),
                summarize(procedureChanges)));
    procedureChangeProposals.saveAllAndFlush(
        procedureChanges.stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    return result(motion);
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<MotionResult> list(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return motions.findProjectionsByPolityId(polityId).stream().map(this::result).toList();
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public MotionResult get(UUID polityId, UUID motionId, UUID userId) {
    access.requireReadable(polityId, userId);
    return result(projection(polityId, motionId));
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult vote(
      UUID polityId, UUID motionId, AuthenticatedUser actor, @Valid CastVoteInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    VoteChoice choice = input.choice();
    Membership voter = membershipReader.active(polityId, actor.id());
    Motion motion = motion(polityId, motionId);
    requireVoting(motion, now);
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
        new OfficialRecordCitation(
            motionId,
            motion.getProcedureId(),
            motion.getInstitutionId(),
            null,
            null,
            motion.getEffectType(),
            "vote_cast"),
        "Vote recorded on " + motion.getTitle(),
        voter.getDisplayName() + " cast or updated a ballot while voting was open.",
        now);
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult certify(UUID polityId, UUID motionId, AuthenticatedUser actor) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Membership requester = membershipReader.active(polityId, actor.id());
    Motion motion = motion(polityId, motionId);
    requireVotingStatus(motion);
    requireCertificationOpen(motion, now);
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
    VotingResult outcome =
        voting.evaluate(
            procedure,
            Math.toIntExact(electors.countByMotionId(motionId)),
            votes.findByMotionId(motionId));
    Certification certification =
        certifications.saveAndFlush(
            new Certification(polityId, motionId, requester.getId(), outcome, now));
    motion.certify(outcome.passed(), now);
    motions.saveAndFlush(motion);
    record.append(
        polityId,
        motion.getJurisdictionId(),
        constitution.getId(),
        requester.getId(),
        OfficialRecordType.MOTION_CERTIFIED,
        certification.getId(),
        new OfficialRecordCitation(
            motionId,
            motion.getProcedureId(),
            motion.getInstitutionId(),
            PowerCode.REQUEST_CERTIFICATION,
            certification.getId(),
            motion.getEffectType(),
            outcome.passed() ? "passed" : "rejected"),
        "Result certified: " + motion.getTitle(),
        outcome.explanation(),
        now);
    if (outcome.passed()) {
      effects.apply(motion, requester, constitution, now);
    } else {
      record.append(
          polityId,
          motion.getJurisdictionId(),
          constitution.getId(),
          requester.getId(),
          OfficialRecordType.MOTION_REJECTED,
          motionId,
          new OfficialRecordCitation(
              motionId,
              motion.getProcedureId(),
              motion.getInstitutionId(),
              PowerCode.REQUEST_CERTIFICATION,
              certification.getId(),
              motion.getEffectType(),
              "rejected"),
          "Motion rejected: " + motion.getTitle(),
          "The certified result did not authorize the proposed effect.",
          now);
    }
    return result(motion);
  }

  private MotionResult result(Motion motion) {
    return result(projection(motion.getPolityId(), motion.getId()));
  }

  private MotionResult result(MotionProjection motion) {
    VotingResult tally =
        voting.evaluate(
            motion,
            Math.toIntExact(electors.countByMotionId(motion.getId())),
            votes.findByMotionId(motion.getId()));
    return mapper.toResult(
        motion, tally, certifications.findByMotionId(motion.getId()).orElse(null));
  }

  private MotionProjection projection(UUID polityId, UUID motionId) {
    return motions
        .findProjectedByIdAndPolityId(motionId, polityId)
        .orElseThrow(() -> ApiException.notFound("motion_not_found", "Motion not found."));
  }

  private Motion motion(UUID polityId, UUID motionId) {
    return motions
        .findByIdAndPolityId(motionId, polityId)
        .orElseThrow(() -> ApiException.notFound("motion_not_found", "Motion not found."));
  }

  private Procedure procedure(UUID constitutionId, String code) {
    return procedures
        .findByConstitutionVersionIdAndCode(constitutionId, code)
        .orElseThrow(() -> ApiException.notFound("procedure_not_found", "Procedure not found."));
  }

  private void requireVotingStatus(Motion motion) {
    if (motion.getStatus() != MotionStatus.VOTING) {
      throw ApiException.conflict("motion_not_voting", "This motion is no longer open for voting.");
    }
  }

  private void requireVoting(Motion motion, OffsetDateTime now) {
    requireVotingStatus(motion);
    if (now.isBefore(motion.getVotingOpensAt())) {
      throw ApiException.conflict("voting_not_open", "Voting has not opened for this motion.");
    }
    if (!now.isBefore(motion.getVotingClosesAt())) {
      throw ApiException.conflict("voting_closed", "Voting has closed for this motion.");
    }
  }

  private void requireCertificationOpen(Motion motion, OffsetDateTime now) {
    if (now.isBefore(motion.getCertificationOpensAt())) {
      throw ApiException.conflict(
          "certification_not_open", "This motion cannot be certified until voting closes.");
    }
  }

  private Motion createMotion(
      UUID polityId,
      Membership introducer,
      ConstitutionVersion constitution,
      Jurisdiction jurisdiction,
      Institution institution,
      Procedure procedure,
      String title,
      String body,
      PowerCode introducingPower,
      OffsetDateTime now) {
    OffsetDateTime votingOpensAt = now.plusHours(procedure.getMinimumNoticeHours());
    OffsetDateTime votingClosesAt = votingOpensAt.plusHours(procedure.getVotingPeriodHours());
    Motion motion =
        motions.saveAndFlush(
            new Motion(
                polityId,
                jurisdiction.getId(),
                institution.getId(),
                constitution.getId(),
                procedure.getId(),
                introducer.getId(),
                title,
                body,
                procedure.getEffectType(),
                now,
                votingOpensAt,
                votingClosesAt,
                votingClosesAt));
    List<Membership> eligible =
        memberships
            .findByPolityIdAndStatusOrderByAdmittedAtAsc(polityId, MembershipStatus.ACTIVE)
            .stream()
            .filter(member -> standing.hasPoliticalStanding(member, votingOpensAt))
            .toList();
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
        new OfficialRecordCitation(
            motion.getId(),
            procedure.getId(),
            institution.getId(),
            introducingPower,
            null,
            procedure.getEffectType(),
            "introduced"),
        title,
        "%s introduced %s for the %s under Constitution v%d. Voting opens at %s with %d eligible citizens."
            .formatted(
                introducer.getDisplayName(),
                procedure.getName(),
                institution.getName(),
                constitution.getVersion(),
                votingOpensAt,
                eligible.size()),
        now);
    return motion;
  }

  private Membership activeMembership(UUID membershipId, UUID polityId) {
    return memberships
        .findById(membershipId)
        .filter(member -> member.getPolityId().equals(polityId))
        .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
        .orElseThrow(() -> ApiException.notFound("member_not_found", "Member not found."));
  }

  private void requireAppealable(UUID polityId, Sanction sanction, OffsetDateTime now) {
    if (!sanction.isActiveAt(now)) {
      throw ApiException.conflict("sanction_not_active", "Only active sanctions can be appealed.");
    }
    if (appeals.existsByPolityIdAndSanctionId(polityId, sanction.getId())) {
      throw ApiException.conflict(
          "appeal_already_granted", "This sanction has already been appealed.");
    }
    if (appealProposals.existsByPolityIdAndSanctionIdAndMotionStatus(
        polityId, sanction.getId(), MotionStatus.VOTING)) {
      throw ApiException.conflict(
          "appeal_already_open", "This sanction already has an open appeal motion.");
    }
  }

  private List<CreateProcedureChangeInput> validateProcedureChanges(
      CreateConstitutionAmendmentMotionInput input, ConstitutionVersion constitution) {
    Set<String> procedureCodes = new HashSet<>();
    return input.procedureChanges().stream()
        .peek(
            change -> {
              if (!procedureCodes.add(change.procedureCode())) {
                throw ApiException.badRequest(
                    "duplicate_procedure_change",
                    "Each procedure can appear only once in an amendment.");
              }
              procedure(constitution.getId(), change.procedureCode());
            })
        .toList();
  }

  private ConstitutionProcedureChangeProposal toProposal(
      UUID polityId, UUID amendmentProposalId, CreateProcedureChangeInput change) {
    return new ConstitutionProcedureChangeProposal(
        polityId,
        amendmentProposalId,
        change.procedureCode(),
        change.quorumNumerator(),
        change.quorumDenominator(),
        change.threshold(),
        change.minimumNoticeHours(),
        change.votingPeriodHours());
  }

  private String summarize(List<CreateProcedureChangeInput> changes) {
    return changes.stream()
        .map(
            change ->
                "%s(%s)"
                    .formatted(
                        change.procedureCode(),
                        List.of(
                                part("quorum", quorum(change)),
                                part("threshold", change.threshold()),
                                part("minimumNoticeHours", change.minimumNoticeHours()),
                                part("votingPeriodHours", change.votingPeriodHours()))
                            .stream()
                            .filter(part -> !part.isBlank())
                            .reduce((left, right) -> left + ", " + right)
                            .orElse("")))
        .reduce((left, right) -> left + "; " + right)
        .orElse("");
  }

  private Object quorum(CreateProcedureChangeInput change) {
    if (change.quorumNumerator() == null) {
      return null;
    }
    return change.quorumNumerator() + "/" + change.quorumDenominator();
  }

  private String part(String label, Object value) {
    return value == null ? "" : label + "=" + value;
  }
}
