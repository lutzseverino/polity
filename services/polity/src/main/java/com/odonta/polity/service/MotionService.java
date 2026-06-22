package com.odonta.polity.service;

import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.evaluator.VotingEvaluator;
import com.odonta.polity.mapper.MotionApplicationMapper;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.CastOfficeElectionBallotInput;
import com.odonta.polity.model.CastVoteInput;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.CreateAppealMotionInput;
import com.odonta.polity.model.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.model.CreateDisbandmentMotionInput;
import com.odonta.polity.model.CreateMotionInput;
import com.odonta.polity.model.CreateOfficeChangeInput;
import com.odonta.polity.model.CreateOfficeElectionMotionInput;
import com.odonta.polity.model.CreatePowerChangeInput;
import com.odonta.polity.model.CreateProcedureChangeInput;
import com.odonta.polity.model.CreateSanctionMotionInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.MotionResult;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.MotionTemplateKey;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallot;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateResult;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionProposal;
import com.odonta.polity.model.OfficeElectionResult;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.RespondOfficeElectionCandidacyInput;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionProposal;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.repository.VoteRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
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
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals;
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals;
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final EffectApplicationService effects;
  private final MotionElectorRepository electors;
  private final MembershipService membershipService;
  private final MembershipRepository memberships;
  private final MotionApplicationMapper mapper;
  private final MotionRepository motions;
  private final OfficeElectionBallotRepository officeElectionBallots;
  private final OfficeElectionCandidateRepository officeElectionCandidates;
  private final OfficeElectionEvaluator officeElections;
  private final OfficeElectionProposalRepository officeElectionProposals;
  private final OfficeRepository offices;
  private final OfficialRecordService officialRecords;
  private final PolityService polities;
  private final ProcedureElectorateService procedureElectorates;
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
    polities.requireActive(polityId);
    Membership introducer = membershipService.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_MOTION);
    polities.requireFullGovernment(polityId);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.ORDINARY_RESOLUTION);
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            input.title(),
            input.body(),
            null,
            PowerCode.INTRODUCE_MOTION,
            now);
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult createOfficeElection(
      UUID polityId, AuthenticatedUser actor, @Valid CreateOfficeElectionMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership introducer = membershipService.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_OFFICE_ELECTION);
    polities.requireFullGovernment(polityId);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.OFFICE_ELECTION);
    Office office = currentOffice(input.officeId(), polityId, constitution);
    List<Membership> candidates = activeCandidates(input.candidateMembershipIds(), polityId, now);
    List<String> candidateNames = candidates.stream().map(Membership::getDisplayName).toList();
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.OFFICE_ELECTION,
            Map.of(
                "officeName", office.getName(),
                "candidateNames", candidateNames,
                "candidateCount", candidates.size()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.fallbackTitle(),
            template.fallbackBody(),
            template,
            PowerCode.INTRODUCE_OFFICE_ELECTION,
            now);
    officeElectionProposals.saveAndFlush(
        new OfficeElectionProposal(polityId, motion.getId(), office.getId()));
    officeElectionCandidates.saveAllAndFlush(
        candidates.stream()
            .map(
                candidate ->
                    new OfficeElectionCandidate(
                        polityId,
                        motion.getId(),
                        candidate.getId(),
                        candidate.getId().equals(introducer.getId())
                            ? OfficeElectionCandidateStatus.ACCEPTED
                            : OfficeElectionCandidateStatus.PENDING,
                        candidate.getId().equals(introducer.getId()) ? now : null))
            .toList());
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult createSanction(
      UUID polityId, AuthenticatedUser actor, @Valid CreateSanctionMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership introducer = membershipService.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_SANCTION);
    polities.requireFullGovernment(polityId);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.SANCTION);
    Membership target = activeMembership(input.targetMembershipId(), polityId);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.SANCTION,
            Map.of(
                "targetName",
                target.getDisplayName(),
                "sanctionType",
                input.type().name(),
                "reason",
                input.reason(),
                "durationDays",
                input.durationDays()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.fallbackTitle(),
            template.fallbackBody(),
            template,
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
    polities.requireActive(polityId);
    Membership introducer = membershipService.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    Sanction sanction =
        sanctions
            .findByIdAndPolityId(input.sanctionId(), polityId)
            .orElseThrow(() -> ApiException.notFound("sanction_not_found", "Sanction not found."));
    requireAppealable(polityId, sanction, now);
    if (sanction.getTargetMembershipId().equals(introducer.getId())) {
      authority.requireOwnAppealIntroduction(introducer, constitution);
    } else {
      authority.require(introducer, constitution, PowerCode.INTRODUCE_APPEAL);
      polities.requireFullGovernment(polityId);
    }
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.APPEAL);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.APPEAL,
            Map.of("reason", input.reason(), "sanctionId", sanction.getId().toString()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.fallbackTitle(),
            template.fallbackBody(),
            template,
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
    polities.requireActive(polityId);
    Membership introducer = membershipService.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_AMENDMENT);
    polities.requireFullGovernment(polityId);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.CONSTITUTION_AMENDMENT);
    List<CreateOfficeChangeInput> officeChanges =
        requireProposableOfficeChanges(input, constitution);
    List<CreateProcedureChangeInput> procedureChanges =
        requireProposableProcedureChanges(input, constitution, officeChanges);
    List<CreatePowerChangeInput> powerChanges =
        requireProposablePowerChanges(input, constitution, officeChanges);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.CONSTITUTION_AMENDMENT,
            Map.of("title", input.title(), "body", input.body()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.fallbackTitle(),
            template.fallbackBody(),
            template,
            PowerCode.INTRODUCE_AMENDMENT,
            now);
    ConstitutionAmendmentProposal proposal =
        amendmentProposals.saveAndFlush(
            new ConstitutionAmendmentProposal(
                polityId,
                motion.getId(),
                input.title(),
                input.body(),
                summarize(procedureChanges, officeChanges, powerChanges)));
    procedureChangeProposals.saveAllAndFlush(
        procedureChanges.stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    officeChangeProposals.saveAllAndFlush(
        officeChanges.stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    powerChangeProposals.saveAllAndFlush(
        powerChanges.stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult createDisbandment(
      UUID polityId, AuthenticatedUser actor, @Valid CreateDisbandmentMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership introducer = membershipService.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_DISBANDMENT);
    polities.requireDisbandmentGovernment(polityId);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Institution institution = polities.institution(polityId, constitution);
    Procedure procedure = procedure(constitution.getId(), Procedure.DISBANDMENT);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.DISBANDMENT, Map.of("title", input.title(), "body", input.body()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.fallbackTitle(),
            template.fallbackBody(),
            template,
            PowerCode.INTRODUCE_DISBANDMENT,
            now);
    return result(motion);
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<MotionResult> list(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    return motions.findProjectionsByPolityIdOrderByOpenedAtDesc(polityId).stream()
        .map(this::result)
        .toList();
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
    polities.requireActive(polityId);
    VoteChoice choice = input.choice();
    Membership voter = membershipService.active(polityId, actor.id());
    Motion motion = motion(polityId, motionId);
    if (motion.getEffectType() == EffectType.ELECT_OFFICE) {
      throw ApiException.conflict(
          "office_election_ballot_required",
          "Office elections require an election ballot, not a yes/no vote.");
    }
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
    officialRecords.append(
        polityId,
        motion.getJurisdictionId(),
        motion.getConstitutionVersionId(),
        voter.getId(),
        OfficialRecordType.VOTE_CAST,
        motionId,
        OfficialRecordContext.motion(motion, OfficialRecordOutcome.VOTE_CAST),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.VOTE_CAST,
            Map.of("motionTitle", motion.getTitle(), "voterName", voter.getDisplayName())),
        now);
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult castOfficeElectionBallot(
      UUID polityId,
      UUID motionId,
      AuthenticatedUser actor,
      @Valid CastOfficeElectionBallotInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership voter = membershipService.active(polityId, actor.id());
    Motion motion = motion(polityId, motionId);
    if (motion.getEffectType() != EffectType.ELECT_OFFICE) {
      throw ApiException.conflict(
          "motion_not_office_election", "This motion is not an office election.");
    }
    requireVoting(motion, now);
    if (!electors.existsByMotionIdAndMembershipId(motionId, voter.getId())) {
      throw ApiException.forbidden(
          "vote_ineligible", "This member was not eligible when voting opened.");
    }
    if (!officeElectionCandidates.existsByMotionIdAndMembershipIdAndStatus(
        motionId, input.candidateMembershipId(), OfficeElectionCandidateStatus.ACCEPTED)) {
      throw ApiException.badRequest(
          "candidate_not_accepted", "This member is not an accepted candidate in the election.");
    }
    OfficeElectionBallot ballot =
        officeElectionBallots
            .findByMotionIdAndMembershipId(motionId, voter.getId())
            .map(
                existing -> {
                  existing.replace(input.candidateMembershipId(), now);
                  return existing;
                })
            .orElseGet(
                () ->
                    new OfficeElectionBallot(
                        polityId, motionId, voter.getId(), input.candidateMembershipId(), now));
    officeElectionBallots.saveAndFlush(ballot);
    officialRecords.append(
        polityId,
        motion.getJurisdictionId(),
        motion.getConstitutionVersionId(),
        voter.getId(),
        OfficialRecordType.VOTE_CAST,
        motionId,
        OfficialRecordContext.motion(motion, OfficialRecordOutcome.ELECTION_BALLOT_CAST),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.OFFICE_ELECTION_BALLOT_CAST,
            Map.of("motionTitle", motion.getTitle(), "voterName", voter.getDisplayName())),
        now);
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult respondOfficeElectionCandidacy(
      UUID polityId,
      UUID motionId,
      AuthenticatedUser actor,
      @Valid RespondOfficeElectionCandidacyInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership candidate = membershipService.active(polityId, actor.id());
    Motion motion = motion(polityId, motionId);
    if (motion.getEffectType() != EffectType.ELECT_OFFICE) {
      throw ApiException.conflict(
          "motion_not_office_election", "This motion is not an office election.");
    }
    requireCandidacyResponseOpen(motion, now);
    OfficeElectionCandidate candidacy =
        officeElectionCandidates
            .findByMotionIdAndMembershipId(motionId, candidate.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "candidacy_not_found", "This member is not nominated in the election."));
    if (input.accepted()) {
      membershipService.requirePoliticalStanding(candidate, now);
    }
    candidacy.respond(input.accepted(), now);
    officeElectionCandidates.saveAndFlush(candidacy);
    OfficialRecordTemplateKey responseTemplateKey =
        input.accepted()
            ? OfficialRecordTemplateKey.CANDIDACY_ACCEPTED
            : OfficialRecordTemplateKey.CANDIDACY_DECLINED;
    OfficialRecordOutcome outcome =
        input.accepted()
            ? OfficialRecordOutcome.CANDIDACY_ACCEPTED
            : OfficialRecordOutcome.CANDIDACY_DECLINED;
    officialRecords.append(
        polityId,
        motion.getJurisdictionId(),
        motion.getConstitutionVersionId(),
        candidate.getId(),
        OfficialRecordType.CANDIDACY_RESPONDED,
        motionId,
        OfficialRecordContext.motion(motion, outcome),
        OfficialRecordTemplate.of(
            responseTemplateKey,
            Map.of("motionTitle", motion.getTitle(), "candidateName", candidate.getDisplayName())),
        now);
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult certify(UUID polityId, UUID motionId, AuthenticatedUser actor) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership requester = membershipService.active(polityId, actor.id());
    Motion motion = motion(polityId, motionId);
    requireVotingStatus(motion);
    requireCertificationOpen(motion, now);
    ConstitutionVersion constitution =
        constitutions
            .findById(motion.getConstitutionVersionId())
            .orElseThrow(
                () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
    if (constitution.getStatus() != ConstitutionStatus.RATIFIED) {
      throw ApiException.conflict(
          "constitution_superseded",
          "This motion was introduced under a constitution that is no longer ratified.");
    }
    if (canCertifyOwnAppealWithoutStanding(motion, requester)) {
      authority.requireAppealCertification(requester, constitution);
    } else {
      authority.require(requester, constitution, PowerCode.REQUEST_CERTIFICATION);
    }
    Procedure procedure =
        procedures
            .findById(motion.getProcedureId())
            .orElseThrow(
                () -> ApiException.notFound("procedure_not_found", "Procedure not found."));
    int eligible = Math.toIntExact(electors.countByMotionId(motionId));
    boolean passed;
    Certification certification;
    if (motion.getEffectType() == EffectType.ELECT_OFFICE) {
      disqualifyOfficeElectionCandidatesWithoutStanding(motionId, now);
      OfficeElectionTallyResult electionOutcome =
          officeElections.evaluate(
              procedure,
              eligible,
              electionCandidates(motionId, false, now),
              officeElectionBallots.findByMotionId(motionId));
      passed = electionOutcome.passed();
      certification =
          certifications.saveAndFlush(
              new Certification(polityId, motionId, requester.getId(), electionOutcome, now));
    } else {
      VotingResult outcome = voting.evaluate(procedure, eligible, votes.findByMotionId(motionId));
      passed = outcome.passed();
      certification =
          certifications.saveAndFlush(
              new Certification(polityId, motionId, requester.getId(), outcome, now));
    }
    motion.certify(passed, now);
    motions.saveAndFlush(motion);
    OfficialRecordOutcome certificationOutcome =
        passed ? OfficialRecordOutcome.PASSED : OfficialRecordOutcome.REJECTED;
    officialRecords.append(
        polityId,
        motion.getJurisdictionId(),
        constitution.getId(),
        requester.getId(),
        OfficialRecordType.MOTION_CERTIFIED,
        certification.getId(),
        OfficialRecordContext.certification(
            motion, PowerCode.REQUEST_CERTIFICATION, certification.getId(), certificationOutcome),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.MOTION_CERTIFIED,
            Map.of(
                "motionTitle",
                motion.getTitle(),
                "outcome",
                certificationOutcome.value(),
                "outcomeReason",
                certification.getOutcomeReason().name())),
        now);
    if (passed) {
      effects.apply(motion, requester, constitution, now);
    } else {
      officialRecords.append(
          polityId,
          motion.getJurisdictionId(),
          constitution.getId(),
          requester.getId(),
          OfficialRecordType.MOTION_REJECTED,
          motionId,
          OfficialRecordContext.certification(
              motion,
              PowerCode.REQUEST_CERTIFICATION,
              certification.getId(),
              OfficialRecordOutcome.REJECTED),
          OfficialRecordTemplate.of(
              OfficialRecordTemplateKey.MOTION_REJECTED, Map.of("motionTitle", motion.getTitle())),
          now);
    }
    return result(motion);
  }

  private MotionResult result(Motion motion) {
    return result(projection(motion.getPolityId(), motion.getId()));
  }

  private MotionResult result(MotionProjection motion) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Procedure procedure = procedure(motion.getProcedureId());
    VotingResult tally =
        motion.getEffectType() == EffectType.ELECT_OFFICE
            ? null
            : voting.evaluate(
                procedure,
                Math.toIntExact(electors.countByMotionId(motion.getId())),
                votes.findByMotionId(motion.getId()));
    OfficeElectionTallyResult electionTally =
        motion.getEffectType() == EffectType.ELECT_OFFICE
            ? officeElections.evaluate(
                procedure,
                Math.toIntExact(electors.countByMotionId(motion.getId())),
                electionCandidates(motion.getId(), motion.getStatus() == MotionStatus.VOTING, now),
                officeElectionBallots.findByMotionId(motion.getId()))
            : null;
    Certification certification = certifications.findByMotionId(motion.getId()).orElse(null);
    ConstitutionVersion constitution =
        constitutions
            .findById(motion.getConstitutionVersionId())
            .orElseThrow(
                () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
    return new MotionResult(
        motion.getId(),
        motion.getTitle(),
        motion.getBody(),
        motion.getTitleKey(),
        motion.getBodyKey(),
        motion.getTemplateParams(),
        motion.getStatus(),
        motion.getEffectType(),
        constitution.getVersion(),
        procedure.getName(),
        procedure.getNameKey(),
        membershipService.displayName(motion.getIntroducedBy()),
        motion.getOpenedAt(),
        motion.getVotingOpensAt(),
        motion.getVotingClosesAt(),
        motion.getCertificationOpensAt(),
        tally,
        officeElection(motion),
        electionTally,
        certification == null ? null : mapper.toResult(certification));
  }

  private OfficeElectionResult officeElection(MotionProjection motion) {
    if (motion.getEffectType() != EffectType.ELECT_OFFICE) {
      return null;
    }
    return officeElectionProposals
        .findByMotionId(motion.getId())
        .map(
            proposal -> {
              Office office =
                  offices
                      .findByIdAndPolityId(proposal.getOfficeId(), proposal.getPolityId())
                      .orElseThrow(
                          () -> ApiException.notFound("office_not_found", "Office not found."));
              List<OfficeElectionCandidateResult> candidates =
                  officeElectionCandidates.findByMotionId(motion.getId()).stream()
                      .map(
                          candidate -> {
                            Membership membership =
                                membershipService.get(candidate.getMembershipId());
                            return new OfficeElectionCandidateResult(
                                membership.getId(),
                                membership.getDisplayName(),
                                candidate.getStatus(),
                                candidate.getRespondedAt());
                          })
                      .sorted(
                          java.util.Comparator.comparing(OfficeElectionCandidateResult::name)
                              .thenComparing(candidate -> candidate.membershipId().toString()))
                      .toList();
              return new OfficeElectionResult(
                  office.getId(),
                  office.getCode(),
                  office.getName(),
                  office.getNameKey(),
                  candidates);
            })
        .orElse(null);
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

  private Procedure procedure(UUID procedureId) {
    return procedures
        .findById(procedureId)
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

  private void requireCandidacyResponseOpen(Motion motion, OffsetDateTime now) {
    requireVotingStatus(motion);
    if (!now.isBefore(motion.getVotingOpensAt())) {
      throw ApiException.conflict(
          "candidacy_response_closed", "Candidate responses close when voting opens.");
    }
  }

  private void requireCertificationOpen(Motion motion, OffsetDateTime now) {
    if (now.isBefore(motion.getCertificationOpensAt())) {
      throw ApiException.conflict(
          "certification_not_open", "This motion cannot be certified until voting closes.");
    }
  }

  private Motion introduceMotion(
      UUID polityId,
      Membership introducer,
      ConstitutionVersion constitution,
      Jurisdiction jurisdiction,
      Institution institution,
      Procedure procedure,
      String title,
      String body,
      MotionTemplate template,
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
                template,
                procedure.getEffectType(),
                now,
                votingOpensAt,
                votingClosesAt,
                votingClosesAt));
    List<Membership> eligible = procedureElectorates.electors(procedure, votingOpensAt);
    electors.saveAllAndFlush(
        eligible.stream()
            .map(member -> new MotionElector(polityId, motion.getId(), member.getId()))
            .toList());
    officialRecords.append(
        polityId,
        jurisdiction.getId(),
        constitution.getId(),
        introducer.getId(),
        OfficialRecordType.MOTION_INTRODUCED,
        motion.getId(),
        OfficialRecordContext.motion(motion, introducingPower, OfficialRecordOutcome.INTRODUCED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.MOTION_INTRODUCED,
            Map.of(
                "motionTitle",
                title,
                "introducerName",
                introducer.getDisplayName(),
                "procedureName",
                procedure.getName(),
                "institutionName",
                institution.getName(),
                "constitutionVersion",
                constitution.getVersion(),
                "votingOpensAt",
                votingOpensAt.toString(),
                "eligibleElectorCount",
                eligible.size())),
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

  private Office currentOffice(UUID officeId, UUID polityId, ConstitutionVersion constitution) {
    Office office =
        offices
            .findByIdAndPolityId(officeId, polityId)
            .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
    if (!office.getConstitutionVersionId().equals(constitution.getId())) {
      throw ApiException.conflict(
          "office_not_current", "This office belongs to a previous constitution version.");
    }
    return office;
  }

  private List<Membership> activeCandidates(
      List<UUID> candidateMembershipIds, UUID polityId, OffsetDateTime now) {
    Set<UUID> candidateIds = new HashSet<>();
    return candidateMembershipIds.stream()
        .map(
            candidateId -> {
              if (!candidateIds.add(candidateId)) {
                throw ApiException.badRequest(
                    "duplicate_candidate", "Each candidate can appear only once in an election.");
              }
              Membership candidate = activeMembership(candidateId, polityId);
              membershipService.requirePoliticalStanding(candidate, now);
              return candidate;
            })
        .toList();
  }

  private boolean canCertifyOwnAppealWithoutStanding(Motion motion, Membership requester) {
    return motion.getEffectType() == EffectType.GRANT_APPEAL
        && appealProposals
            .findByMotionId(motion.getId())
            .map(proposal -> proposal.getAppellantMembershipId().equals(requester.getId()))
            .orElse(false);
  }

  private void disqualifyOfficeElectionCandidatesWithoutStanding(
      UUID motionId, OffsetDateTime now) {
    List<OfficeElectionCandidate> disqualified =
        officeElectionCandidates
            .findByMotionIdAndStatus(motionId, OfficeElectionCandidateStatus.ACCEPTED)
            .stream()
            .filter(
                candidate -> {
                  Membership membership =
                      activeMembership(candidate.getMembershipId(), candidate.getPolityId());
                  return !membershipService.hasPoliticalStanding(membership, now);
                })
            .toList();
    disqualified.forEach(candidate -> candidate.disqualify(now));
    if (!disqualified.isEmpty()) {
      officeElectionCandidates.saveAllAndFlush(disqualified);
    }
  }

  private List<OfficeElectionCandidateOption> electionCandidates(
      UUID motionId, boolean requireStanding, OffsetDateTime now) {
    return officeElectionCandidates
        .findByMotionIdAndStatus(motionId, OfficeElectionCandidateStatus.ACCEPTED)
        .stream()
        .map(
            candidate -> {
              Membership membership =
                  activeMembership(candidate.getMembershipId(), candidate.getPolityId());
              if (requireStanding && !membershipService.hasPoliticalStanding(membership, now)) {
                return null;
              }
              return new OfficeElectionCandidateOption(
                  membership.getId(), membership.getDisplayName());
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private void requireAppealable(UUID polityId, Sanction sanction, OffsetDateTime now) {
    if (sanction.isInactiveAt(now)) {
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

  private List<CreateProcedureChangeInput> requireProposableProcedureChanges(
      CreateConstitutionAmendmentMotionInput input,
      ConstitutionVersion constitution,
      List<CreateOfficeChangeInput> officeChanges) {
    List<CreateProcedureChangeInput> changes = changesOrEmpty(input.procedureChanges());
    if (changes.isEmpty()) {
      validateProcedureOfficeReferences(
          constitution, resultingOfficeCodes(constitution, officeChanges), Map.of());
      return List.of();
    }
    Set<String> officeCodes = resultingOfficeCodes(constitution, officeChanges);
    changes =
        changes.stream()
            .map(
                change -> {
                  Procedure procedure = procedure(constitution.getId(), change.procedureCode());
                  if (change.threshold() == VotingThreshold.PLURALITY_CAST
                      && procedure.getEffectType() != EffectType.ELECT_OFFICE) {
                    throw ApiException.badRequest(
                        "plurality_requires_election",
                        "Plurality thresholds can only be used by office election procedures.");
                  }
                  ProcedureElectorate electorate =
                      valueOr(change.electorate(), procedure.getElectorate());
                  String electorateOfficeCode =
                      valueOr(
                          normalizedProposalOfficeCode(change.electorateOfficeCode()),
                          procedure.getElectorateOfficeCode());
                  requireKnownElectorateOffice(electorate, electorateOfficeCode, officeCodes);
                  return change;
                })
            .toList();
    validateProcedureOfficeReferences(
        constitution, officeCodes, changedProcedureElectorates(changes));
    return changes;
  }

  private List<CreateOfficeChangeInput> requireProposableOfficeChanges(
      CreateConstitutionAmendmentMotionInput input, ConstitutionVersion constitution) {
    List<CreateOfficeChangeInput> changes = changesOrEmpty(input.officeChanges());
    if (changes.isEmpty()) {
      return List.of();
    }
    Map<String, Office> currentOffices = currentOffices(constitution);
    changes.forEach(
        change -> {
          String code = normalizedOfficeCode(change.code());
          Office office = currentOffices.get(code);
          switch (change.action()) {
            case CREATE -> {
              if (office != null) {
                throw ApiException.conflict(
                    "office_already_exists", "This office already exists in the constitution.");
              }
            }
            case REVISE -> {
              if (office == null) {
                throw ApiException.notFound("office_not_found", "Office not found.");
              }
            }
            case RETIRE -> {
              if (office == null) {
                throw ApiException.notFound("office_not_found", "Office not found.");
              }
            }
          }
        });
    return changes;
  }

  private List<CreatePowerChangeInput> requireProposablePowerChanges(
      CreateConstitutionAmendmentMotionInput input,
      ConstitutionVersion constitution,
      List<CreateOfficeChangeInput> officeChanges) {
    List<CreatePowerChangeInput> changes = changesOrEmpty(input.powerChanges());
    if (changes.isEmpty()) {
      validatePowerOfficeReferences(
          constitution, resultingOfficeCodes(constitution, officeChanges), Map.of());
      return List.of();
    }
    Set<String> officeCodes = resultingOfficeCodes(constitution, officeChanges);
    Map<PowerCode, ConstitutionalPower> currentPowers = currentPowers(constitution);
    changes.forEach(
        change -> {
          if (!currentPowers.containsKey(change.powerCode())) {
            throw ApiException.notFound("power_not_found", "Constitutional power not found.");
          }
          requireAllowedPowerHolder(change.powerCode(), change.holderScope());
          requireKnownPowerOffice(
              change.holderScope(), normalizedOfficeCode(change.holderOfficeCode()), officeCodes);
        });
    validatePowerOfficeReferences(constitution, officeCodes, changedPowerHolders(changes));
    return changes;
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
        change.electorate(),
        trimmedProposalValue(change.electorateOfficeCode()),
        change.minimumNoticeHours(),
        change.votingPeriodHours());
  }

  private ConstitutionOfficeChangeProposal toProposal(
      UUID polityId, UUID amendmentProposalId, CreateOfficeChangeInput change) {
    return new ConstitutionOfficeChangeProposal(
        polityId,
        amendmentProposalId,
        change.action(),
        normalizedOfficeCode(change.code()),
        trimmedProposalValue(change.name()),
        trimmedProposalValue(change.description()),
        change.termLengthDays());
  }

  private ConstitutionPowerChangeProposal toProposal(
      UUID polityId, UUID amendmentProposalId, CreatePowerChangeInput change) {
    return new ConstitutionPowerChangeProposal(
        polityId,
        amendmentProposalId,
        change.powerCode(),
        change.holderScope(),
        trimmedProposalValue(change.holderOfficeCode()));
  }

  private String summarize(
      List<CreateProcedureChangeInput> procedureChanges,
      List<CreateOfficeChangeInput> officeChanges,
      List<CreatePowerChangeInput> powerChanges) {
    List<String> summaries = new ArrayList<>();
    summaries.addAll(summarizeProcedureChanges(procedureChanges));
    summaries.addAll(summarizeOfficeChanges(officeChanges));
    summaries.addAll(summarizePowerChanges(powerChanges));
    return String.join("; ", summaries);
  }

  private List<String> summarizeProcedureChanges(List<CreateProcedureChangeInput> changes) {
    return changes.stream()
        .map(
            change ->
                "procedure:%s(%s)"
                    .formatted(
                        change.procedureCode(),
                        Stream.of(
                                part("quorum", quorum(change)),
                                part("threshold", change.threshold()),
                                part("electorate", electorate(change)),
                                part("minimumNoticeHours", change.minimumNoticeHours()),
                                part("votingPeriodHours", change.votingPeriodHours()))
                            .filter(part -> !part.isBlank())
                            .reduce((left, right) -> left + ", " + right)
                            .orElse("")))
        .toList();
  }

  private List<String> summarizeOfficeChanges(List<CreateOfficeChangeInput> changes) {
    return changes.stream()
        .map(
            change ->
                "office:%s(%s)".formatted(normalizedOfficeCode(change.code()), change.action()))
        .toList();
  }

  private List<String> summarizePowerChanges(List<CreatePowerChangeInput> changes) {
    return changes.stream()
        .map(
            change ->
                "power:%s(%s%s)"
                    .formatted(
                        change.powerCode(),
                        change.holderScope(),
                        isBlankProposalValue(change.holderOfficeCode())
                            ? ""
                            : ":" + normalizedOfficeCode(change.holderOfficeCode())))
        .toList();
  }

  private Object quorum(CreateProcedureChangeInput change) {
    if (change.quorumNumerator() == null) {
      return null;
    }
    return change.quorumNumerator() + "/" + change.quorumDenominator();
  }

  private Object electorate(CreateProcedureChangeInput change) {
    if (change.electorate() == null) {
      return null;
    }
    return change.electorate() == ProcedureElectorate.OFFICE_HOLDERS
        ? change.electorate() + ":" + normalizedOfficeCode(change.electorateOfficeCode())
        : change.electorate();
  }

  private String part(String label, Object value) {
    return value == null ? "" : label + "=" + value;
  }

  private Map<String, Office> currentOffices(ConstitutionVersion constitution) {
    return offices.findByConstitutionVersionIdOrderByName(constitution.getId()).stream()
        .collect(java.util.stream.Collectors.toMap(Office::getCode, office -> office));
  }

  private Map<PowerCode, ConstitutionalPower> currentPowers(ConstitutionVersion constitution) {
    return powers.findByConstitutionVersionId(constitution.getId()).stream()
        .collect(java.util.stream.Collectors.toMap(ConstitutionalPower::getCode, power -> power));
  }

  private Set<String> resultingOfficeCodes(
      ConstitutionVersion constitution, List<CreateOfficeChangeInput> officeChanges) {
    Set<String> codes = new HashSet<>(currentOffices(constitution).keySet());
    officeChanges.forEach(
        change -> {
          String code = normalizedOfficeCode(change.code());
          if (change.action() == ConstitutionOfficeChangeAction.RETIRE) {
            codes.remove(code);
          } else {
            codes.add(code);
          }
        });
    return codes;
  }

  private void validatePowerOfficeReferences(
      ConstitutionVersion constitution,
      Set<String> officeCodes,
      Map<PowerCode, CreatePowerChangeInput> powerChanges) {
    currentPowers(constitution)
        .values()
        .forEach(
            power -> {
              CreatePowerChangeInput change = powerChanges.get(power.getCode());
              PowerHolderScope holderScope =
                  change == null ? power.getHolderScope() : change.holderScope();
              String holderOfficeCode =
                  change == null ? power.getHolderOfficeCode() : change.holderOfficeCode();
              requireAllowedPowerHolder(power.getCode(), holderScope);
              requireKnownPowerOffice(
                  holderScope, normalizedOfficeCode(holderOfficeCode), officeCodes);
            });
  }

  private void requireAllowedPowerHolder(PowerCode powerCode, PowerHolderScope holderScope) {
    if (powerCode.requiresActiveMemberHolder() && holderScope != PowerHolderScope.ACTIVE_MEMBER) {
      throw ApiException.badRequest(
          "citizen_power_required",
          "Member initiative, election, appeal, amendment, disbandment, and certification powers must remain active citizen powers.");
    }
  }

  private void requireKnownPowerOffice(
      PowerHolderScope holderScope, String holderOfficeCode, Set<String> officeCodes) {
    if (holderScope == PowerHolderScope.OFFICE && !officeCodes.contains(holderOfficeCode)) {
      throw ApiException.badRequest(
          "power_holder_office_missing",
          "Office-held powers must refer to an office in the amended constitution.");
    }
  }

  private void validateProcedureOfficeReferences(
      ConstitutionVersion constitution,
      Set<String> officeCodes,
      Map<String, CreateProcedureChangeInput> procedureChanges) {
    procedures
        .findByConstitutionVersionId(constitution.getId())
        .forEach(
            procedure -> {
              CreateProcedureChangeInput change = procedureChanges.get(procedure.getCode());
              ProcedureElectorate electorate =
                  change == null
                      ? procedure.getElectorate()
                      : valueOr(change.electorate(), procedure.getElectorate());
              String electorateOfficeCode =
                  change == null
                      ? procedure.getElectorateOfficeCode()
                      : valueOr(
                          normalizedProposalOfficeCode(change.electorateOfficeCode()),
                          procedure.getElectorateOfficeCode());
              requireKnownElectorateOffice(electorate, electorateOfficeCode, officeCodes);
            });
  }

  private void requireKnownElectorateOffice(
      ProcedureElectorate electorate, String electorateOfficeCode, Set<String> officeCodes) {
    if (electorate == ProcedureElectorate.OFFICE_HOLDERS
        && !officeCodes.contains(electorateOfficeCode)) {
      throw ApiException.badRequest(
          "procedure_electorate_office_missing",
          "Office-held procedure electorates must refer to an office in the amended constitution.");
    }
  }

  private Map<PowerCode, CreatePowerChangeInput> changedPowerHolders(
      List<CreatePowerChangeInput> powerChanges) {
    Map<PowerCode, CreatePowerChangeInput> changes = new EnumMap<>(PowerCode.class);
    powerChanges.forEach(change -> changes.put(change.powerCode(), change));
    return changes;
  }

  private Map<String, CreateProcedureChangeInput> changedProcedureElectorates(
      List<CreateProcedureChangeInput> procedureChanges) {
    Map<String, CreateProcedureChangeInput> changes = new java.util.HashMap<>();
    procedureChanges.forEach(change -> changes.put(change.procedureCode(), change));
    return changes;
  }

  private <T> T valueOr(T value, T fallback) {
    return value == null ? fallback : value;
  }

  private <T> List<T> changesOrEmpty(List<T> values) {
    return values == null ? List.of() : values;
  }

  private String normalizedOfficeCode(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizedProposalOfficeCode(String value) {
    return isBlankProposalValue(value) ? null : normalizedOfficeCode(value);
  }

  private String trimmedProposalValue(String value) {
    return isBlankProposalValue(value) ? null : value.trim();
  }

  private boolean isBlankProposalValue(String value) {
    return value == null || value.isBlank();
  }
}
