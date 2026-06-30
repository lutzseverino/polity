package com.odonta.polity.service;

import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.effect.MotionEffectApplier;
import com.odonta.polity.effect.OfficialActVoidRemedy;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.evaluator.VotingEvaluator;
import com.odonta.polity.mapper.MotionApplicationMapper;
import com.odonta.polity.model.ActionAvailabilityResult;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.CastOfficeElectionBallotInput;
import com.odonta.polity.model.CastVoteInput;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionInstitutionChangeProposal;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.ConstitutionalReviewProposal;
import com.odonta.polity.model.CreateAppealMotionInput;
import com.odonta.polity.model.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.model.CreateConstitutionalReviewMotionInput;
import com.odonta.polity.model.CreateDisbandmentMotionInput;
import com.odonta.polity.model.CreateInstitutionChangeInput;
import com.odonta.polity.model.CreateMotionInput;
import com.odonta.polity.model.CreateOfficeChangeInput;
import com.odonta.polity.model.CreateOfficeElectionMotionInput;
import com.odonta.polity.model.CreateOfficeTermReviewMotionInput;
import com.odonta.polity.model.CreatePowerChangeInput;
import com.odonta.polity.model.CreateProcedureChangeInput;
import com.odonta.polity.model.CreateSanctionMotionInput;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
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
import com.odonta.polity.model.OfficeElectionBallotPreference;
import com.odonta.polity.model.OfficeElectionBallotRanking;
import com.odonta.polity.model.OfficeElectionBallotResult;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateResult;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.OfficeElectionProposal;
import com.odonta.polity.model.OfficeElectionResult;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermReviewProposal;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordEntry;
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
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.model.VotingThreshold;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.ConstitutionalReviewProposalRepository;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.OfficeElectionBallotPreferenceRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalProjection;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProposalRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.repository.VoteRepository;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
  private final ConstitutionInstitutionChangeProposalRepository institutionChangeProposals;
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals;
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals;
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final ConstitutionalReviewProposalRepository constitutionalReviewProposals;
  private final ConstitutionalReviewRepository constitutionalReviews;
  private final MotionEffectApplier effects;
  private final InstitutionRepository institutions;
  private final JurisdictionRepository jurisdictions;
  private final OfficeTermReviewProposalRepository officeTermReviewProposals;
  private final MotionElectorRepository electors;
  private final MembershipService membershipService;
  private final MembershipRepository memberships;
  private final MotionApplicationMapper mapper;
  private final MotionRepository motions;
  private final OfficeElectionBallotRepository officeElectionBallots;
  private final OfficeElectionBallotPreferenceRepository officeElectionBallotPreferences;
  private final OfficeElectionCandidateRepository officeElectionCandidates;
  private final OfficeElectionEvaluator officeElections;
  private final OfficeElectionProposalRepository officeElectionProposals;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficialActVoidRemedy officialActVoidRemedies;
  private final OfficialRecordRepository officialRecordEntries;
  private final OfficialRecordService officialRecords;
  private final PolityService polities;
  private final ProcedureElectorateResolver procedureElectorates;
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
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), Procedure.ORDINARY_RESOLUTION);
    Institution institution = polities.institution(polityId, procedure);
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
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), Procedure.OFFICE_ELECTION);
    Institution institution = polities.institution(polityId, procedure);
    Office office = currentOffice(input.officeId(), polityId, constitution);
    int seatsAvailable = requireOfficeVacancy(office, now);
    List<Membership> candidates = activeCandidates(input.candidateMembershipIds(), polityId, now);
    List<String> candidateNames = candidates.stream().map(Membership::getDisplayName).toList();
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.OFFICE_ELECTION,
            TemplateParameters.of(
                "officeName", office.getName(),
                "officeNameKey", office.getNameKey(),
                "officeCode", office.getCode(),
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
            template.storedTitle(),
            template.storedBody(),
            template,
            PowerCode.INTRODUCE_OFFICE_ELECTION,
            now);
    officeElectionProposals.saveAndFlush(
        new OfficeElectionProposal(
            polityId,
            motion.getId(),
            office.getId(),
            seatsAvailable,
            procedure.getOfficeElectionMethod()));
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
    Membership target = activeMembership(input.targetMembershipId(), polityId);
    requireSanctionSafeguards(introducer, constitution, target, input.durationDays(), now);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), Procedure.SANCTION);
    Institution institution = polities.institution(polityId, procedure);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.SANCTION,
            TemplateParameters.of(
                "targetName",
                target.getDisplayName(),
                "sanctionType",
                input.type().name(),
                "sanctionTypeKey",
                input.type().labelKey(),
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
            template.storedTitle(),
            template.storedBody(),
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
            .findEntityByIdAndPolityId(input.sanctionId(), polityId)
            .orElseThrow(() -> ApiException.notFound("sanction_not_found", "Sanction not found."));
    requireAppealable(polityId, sanction, now);
    if (sanction.getTargetMembershipId().equals(introducer.getId())) {
      authority.requireOwnAppealIntroduction(introducer, constitution);
    } else {
      authority.require(introducer, constitution, PowerCode.INTRODUCE_APPEAL);
    }
    UUID sanctionIntroducerId = motion(polityId, sanction.getMotionId()).getIntroducedBy();
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), Procedure.APPEAL);
    Institution institution = polities.institution(polityId, procedure);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.APPEAL,
            TemplateParameters.of(
                "reason", input.reason(), "sanctionId", sanction.getId().toString()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.storedTitle(),
            template.storedBody(),
            template,
            PowerCode.INTRODUCE_APPEAL,
            recusals(sanction.getTargetMembershipId(), introducer.getId(), sanctionIntroducerId),
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
  public MotionResult createOfficeTermReview(
      UUID polityId, AuthenticatedUser actor, @Valid CreateOfficeTermReviewMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership introducer = membershipService.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_OFFICE_TERM_REVIEW);
    OfficeTerm term = reviewableOfficeTerm(input.officeTermId(), polityId, now);
    requireReviewable(polityId, term);
    Office office =
        offices
            .findEntityByIdAndPolityId(term.getOfficeId(), polityId)
            .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
    Membership officeHolder = membershipService.get(term.getMembershipId());
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), Procedure.OFFICE_TERM_REVIEW);
    Institution institution = polities.institution(polityId, procedure);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.OFFICE_TERM_REVIEW,
            TemplateParameters.of(
                "officeTermId",
                term.getId().toString(),
                "officeName",
                office.getName(),
                "officeNameKey",
                office.getNameKey(),
                "officeCode",
                office.getCode(),
                "memberName",
                officeHolder.getDisplayName(),
                "reason",
                input.reason()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.storedTitle(),
            template.storedBody(),
            template,
            PowerCode.INTRODUCE_OFFICE_TERM_REVIEW,
            recusals(introducer.getId(), term.getMembershipId()),
            now);
    officeTermReviewProposals.saveAndFlush(
        new OfficeTermReviewProposal(
            polityId, motion.getId(), term.getId(), introducer.getId(), input.reason()));
    return result(motion);
  }

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult createConstitutionalReview(
      UUID polityId, AuthenticatedUser actor, @Valid CreateConstitutionalReviewMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership introducer = membershipService.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    authority.require(introducer, constitution, PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW);
    OfficialRecordEntry target = voidableOfficialAct(polityId, input.targetRecordId());
    requireConstitutionalReviewVoidRemedy(polityId, target, now);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), Procedure.CONSTITUTIONAL_REVIEW);
    Institution institution = polities.institution(polityId, procedure);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.CONSTITUTIONAL_REVIEW,
            TemplateParameters.of(
                "entryNumber",
                target.getEntryNumber(),
                "targetType",
                target.getType().storedLabel(),
                "targetTypeCode",
                target.getType().wireValue(),
                "targetTypeKey",
                target.getType().labelKey(),
                "reason",
                input.reason()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.storedTitle(),
            template.storedBody(),
            template,
            PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW,
            recusalsForOfficialAct(introducer.getId(), target),
            now);
    constitutionalReviewProposals.saveAndFlush(
        new ConstitutionalReviewProposal(
            polityId, motion.getId(), target.getId(), introducer.getId(), input.reason()));
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
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), Procedure.CONSTITUTION_AMENDMENT);
    Institution institution = polities.institution(polityId, procedure);
    List<CreateInstitutionChangeInput> institutionChanges =
        requireProposableInstitutionChanges(input, constitution);
    List<CreateOfficeChangeInput> officeChanges =
        requireProposableOfficeChanges(input, constitution, now);
    List<CreateProcedureChangeInput> procedureChanges =
        requireProposableProcedureChanges(input, constitution, institutionChanges, officeChanges);
    List<CreatePowerChangeInput> powerChanges =
        requireProposablePowerChanges(input, constitution, officeChanges);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.CONSTITUTION_AMENDMENT,
            TemplateParameters.of("title", input.title(), "body", input.body()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.storedTitle(),
            template.storedBody(),
            template,
            PowerCode.INTRODUCE_AMENDMENT,
            now);
    ConstitutionAmendmentProposal proposal =
        amendmentProposals.saveAndFlush(
            new ConstitutionAmendmentProposal(
                polityId, motion.getId(), input.title(), input.body()));
    institutionChangeProposals.saveAllAndFlush(
        institutionChanges.stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
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
    Procedure procedure = procedure(constitution.getId(), Procedure.DISBANDMENT);
    Institution institution = polities.institution(polityId, procedure);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.DISBANDMENT,
            TemplateParameters.of("title", input.title(), "body", input.body()));
    Motion motion =
        introduceMotion(
            polityId,
            introducer,
            constitution,
            jurisdiction,
            institution,
            procedure,
            template.storedTitle(),
            template.storedBody(),
            template,
            PowerCode.INTRODUCE_DISBANDMENT,
            now);
    return result(motion);
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public List<MotionResult> list(UUID polityId, UUID userId) {
    access.requireReadable(polityId, userId);
    UUID currentMembershipId = currentMembershipId(polityId, userId);
    return motions.findProjectionsByPolityIdOrderByOpenedAtDesc(polityId).stream()
        .map(motion -> result(motion, currentMembershipId))
        .toList();
  }

  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public MotionResult get(UUID polityId, UUID motionId, UUID userId) {
    access.requireReadable(polityId, userId);
    return result(projection(polityId, motionId), currentMembershipId(polityId, userId));
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
            .findEntityByMotionIdAndMembershipId(motionId, voter.getId())
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
            TemplateParameters.with(
                motion.getTemplateParams(),
                "motionTitle",
                motion.getTitle(),
                "motionTitleKey",
                motion.getTitleKey(),
                "voterName",
                voter.getDisplayName())),
        now);
    return result(motion, voter.getId());
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
    List<UUID> rankedCandidateMembershipIds = rankedCandidateMembershipIds(input);
    requireAcceptedElectionCandidates(motionId, rankedCandidateMembershipIds);
    OfficeElectionBallot ballot =
        officeElectionBallots
            .findEntityByMotionIdAndMembershipId(motionId, voter.getId())
            .map(
                existing -> {
                  existing.replace(now);
                  return existing;
                })
            .orElseGet(() -> new OfficeElectionBallot(polityId, motionId, voter.getId(), now));
    ballot = officeElectionBallots.saveAndFlush(ballot);
    officeElectionBallotPreferences.deleteByBallotId(ballot.getId());
    officeElectionBallotPreferences.saveAllAndFlush(
        preferences(polityId, motionId, ballot, rankedCandidateMembershipIds));
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
            TemplateParameters.with(
                motion.getTemplateParams(),
                "motionTitle",
                motion.getTitle(),
                "motionTitleKey",
                motion.getTitleKey(),
                "voterName",
                voter.getDisplayName())),
        now);
    return result(motion, voter.getId());
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
            .findEntityByMotionIdAndMembershipId(motionId, candidate.getId())
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
            TemplateParameters.with(
                motion.getTemplateParams(),
                "motionTitle",
                motion.getTitle(),
                "motionTitleKey",
                motion.getTitleKey(),
                "candidateName",
                candidate.getDisplayName())),
        now);
    return result(motion, candidate.getId());
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
            .findEntityById(motion.getConstitutionVersionId())
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
            .findEntityById(motion.getProcedureId())
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
              officeElectionSeatsAvailable(motion.getId()),
              officeElectionMethod(motion.getId()),
              electionCandidates(motionId, true, now),
              officeElectionBallotRankings(motionId));
      passed = electionOutcome.passed();
      certification =
          certifications.saveAndFlush(
              new Certification(polityId, motionId, requester.getId(), electionOutcome, now));
    } else {
      VotingResult outcome =
          voting.evaluate(procedure, eligible, votes.findEntitiesByMotionId(motionId));
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
            TemplateParameters.with(
                motion.getTemplateParams(),
                "motionTitle",
                motion.getTitle(),
                "motionTitleKey",
                motion.getTitleKey(),
                "motionBodyKey",
                motion.getBodyKey(),
                "outcome",
                certificationOutcome.value(),
                "outcomeReason",
                certification.getOutcomeReason().name(),
                "outcomeReasonKey",
                certification.getOutcomeReason().labelKey())),
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
              OfficialRecordTemplateKey.MOTION_REJECTED,
              TemplateParameters.with(
                  motion.getTemplateParams(),
                  "motionTitle",
                  motion.getTitle(),
                  "motionTitleKey",
                  motion.getTitleKey(),
                  "motionBodyKey",
                  motion.getBodyKey())),
          now);
    }
    return result(motion, requester.getId());
  }

  private MotionResult result(Motion motion) {
    return result(projection(motion.getPolityId(), motion.getId()), null);
  }

  private MotionResult result(Motion motion, UUID currentMembershipId) {
    return result(projection(motion.getPolityId(), motion.getId()), currentMembershipId);
  }

  private MotionResult result(MotionProjection motion, UUID currentMembershipId) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Procedure procedure = procedure(motion.getProcedureId());
    Certification certification = certifications.findEntityByMotionId(motion.getId()).orElse(null);
    VotingResult tally =
        motion.getEffectType() == EffectType.ELECT_OFFICE
            ? null
            : voting.evaluate(
                procedure,
                Math.toIntExact(electors.countByMotionId(motion.getId())),
                votes.findEntitiesByMotionId(motion.getId()));
    OfficeElectionTallyResult electionTally =
        motion.getEffectType() == EffectType.ELECT_OFFICE
            ? officeElectionTally(motion, procedure, certification, now)
            : null;
    ConstitutionVersion constitution =
        constitutions
            .findEntityById(motion.getConstitutionVersionId())
            .orElseThrow(
                () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
    return mapper.toResult(
        motion,
        constitution.getVersion(),
        procedure.getName(),
        procedure.getNameKey(),
        membershipService.displayName(motion.getIntroducedBy()),
        tally,
        officeElection(motion, currentMembershipId),
        electionTally,
        certification == null ? null : mapper.toResult(certification));
  }

  private OfficeElectionTallyResult officeElectionTally(
      MotionProjection motion,
      Procedure procedure,
      Certification certification,
      OffsetDateTime now) {
    if (certification != null && certification.getElectionTallySnapshot() != null) {
      return certification.getElectionTallySnapshot();
    }
    return officeElections.evaluate(
        procedure,
        Math.toIntExact(electors.countByMotionId(motion.getId())),
        officeElectionSeatsAvailable(motion.getId()),
        officeElectionMethod(motion.getId()),
        electionCandidates(motion.getId(), motion.getStatus() == MotionStatus.VOTING, now),
        officeElectionBallotRankings(motion.getId()));
  }

  private OfficeElectionResult officeElection(MotionProjection motion, UUID currentMembershipId) {
    if (motion.getEffectType() != EffectType.ELECT_OFFICE) {
      return null;
    }
    return officeElectionProposals
        .findProjectedByMotionId(motion.getId())
        .map(
            proposal -> {
              Office office =
                  offices
                      .findEntityByIdAndPolityId(proposal.getOfficeId(), proposal.getPolityId())
                      .orElseThrow(
                          () -> ApiException.notFound("office_not_found", "Office not found."));
              List<OfficeElectionCandidateResult> candidates =
                  officeElectionCandidates.findEntitiesByMotionId(motion.getId()).stream()
                      .map(
                          candidate -> {
                            Membership membership =
                                membershipService.get(candidate.getMembershipId());
                            return mapper.toCandidateResult(
                                membership.getId(),
                                membership.getDisplayName(),
                                candidate.getStatus(),
                                candidate.getRespondedAt());
                          })
                      .sorted(
                          java.util.Comparator.comparing(OfficeElectionCandidateResult::name)
                              .thenComparing(candidate -> candidate.membershipId().toString()))
                      .toList();
              return mapper.toOfficeElectionResult(
                  office.getId(),
                  office.getCode(),
                  office.getName(),
                  office.getNameKey(),
                  proposal.getSeatsAvailable(),
                  proposal.getMethod(),
                  currentOfficeElectionBallot(motion.getId(), currentMembershipId),
                  candidates);
            })
        .orElse(null);
  }

  private UUID currentMembershipId(UUID polityId, UUID userId) {
    return memberships
        .findEntityByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
        .map(Membership::getId)
        .orElse(null);
  }

  private OfficeElectionBallotResult currentOfficeElectionBallot(
      UUID motionId, UUID currentMembershipId) {
    if (currentMembershipId == null) {
      return null;
    }
    return officeElectionBallots
        .findEntityByMotionIdAndMembershipId(motionId, currentMembershipId)
        .map(
            ballot ->
                new OfficeElectionBallotResult(
                    ballot.getCastAt(),
                    currentOfficeElectionRanking(motionId, currentMembershipId)))
        .orElse(null);
  }

  private List<UUID> currentOfficeElectionRanking(UUID motionId, UUID currentMembershipId) {
    return officeElectionBallotPreferences
        .findEntitiesByMotionIdAndMembershipIdOrderByRankAsc(motionId, currentMembershipId)
        .stream()
        .map(OfficeElectionBallotPreference::getCandidateMembershipId)
        .toList();
  }

  private List<UUID> rankedCandidateMembershipIds(CastOfficeElectionBallotInput input) {
    List<UUID> candidateMembershipIds = input.candidateMembershipIds();
    if (candidateMembershipIds == null || candidateMembershipIds.isEmpty()) {
      throw ApiException.badRequest(
          "office_election_ranking_required",
          "Office election ballots must rank at least one accepted candidate.");
    }
    if (new LinkedHashSet<>(candidateMembershipIds).size() != candidateMembershipIds.size()) {
      throw ApiException.badRequest(
          "office_election_ranking_duplicate",
          "Each accepted candidate can appear only once on an election ballot.");
    }
    return candidateMembershipIds;
  }

  private void requireAcceptedElectionCandidates(UUID motionId, List<UUID> candidateMembershipIds) {
    for (UUID candidateMembershipId : candidateMembershipIds) {
      if (!officeElectionCandidates.existsByMotionIdAndMembershipIdAndStatus(
          motionId, candidateMembershipId, OfficeElectionCandidateStatus.ACCEPTED)) {
        throw ApiException.badRequest(
            "candidate_not_accepted", "This member is not an accepted candidate in the election.");
      }
    }
  }

  private List<OfficeElectionBallotPreference> preferences(
      UUID polityId,
      UUID motionId,
      OfficeElectionBallot ballot,
      List<UUID> candidateMembershipIds) {
    List<OfficeElectionBallotPreference> preferences = new java.util.ArrayList<>();
    for (int index = 0; index < candidateMembershipIds.size(); index++) {
      preferences.add(
          new OfficeElectionBallotPreference(
              polityId,
              motionId,
              ballot.getId(),
              ballot.getMembershipId(),
              candidateMembershipIds.get(index),
              index + 1));
    }
    return preferences;
  }

  private int officeElectionSeatsAvailable(UUID motionId) {
    return officeElectionProposals
        .findProjectedByMotionId(motionId)
        .map(OfficeElectionProposalProjection::getSeatsAvailable)
        .orElse(0);
  }

  private OfficeElectionMethod officeElectionMethod(UUID motionId) {
    return officeElectionProposals
        .findProjectedByMotionId(motionId)
        .map(OfficeElectionProposalProjection::getMethod)
        .orElse(OfficeElectionMethod.RANKED_CHOICE);
  }

  private List<OfficeElectionBallotRanking> officeElectionBallotRankings(UUID motionId) {
    Map<UUID, List<UUID>> candidateMembershipIdsByMembershipId = new LinkedHashMap<>();
    officeElectionBallotPreferences
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

  private int requireOfficeVacancy(Office office, OffsetDateTime now) {
    int vacantSeats = vacantSeatCount(office, now);
    if (vacantSeats <= 0) {
      throw ApiException.conflict(
          "office_seats_full", "This office has no vacant seats for another active term.");
    }
    return vacantSeats;
  }

  private int vacantSeatCount(Office office, OffsetDateTime now) {
    long activeTerms =
        officeTerms.countByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            office.getPolityId(), office.getCode(), OfficeTermStatus.ACTIVE, now);
    return Math.max(0, office.getSeatCount() - Math.toIntExact(activeTerms));
  }

  private MotionProjection projection(UUID polityId, UUID motionId) {
    return motions
        .findProjectedByIdAndPolityId(motionId, polityId)
        .orElseThrow(() -> ApiException.notFound("motion_not_found", "Motion not found."));
  }

  private Motion motion(UUID polityId, UUID motionId) {
    return motions
        .findEntityByIdAndPolityId(motionId, polityId)
        .orElseThrow(() -> ApiException.notFound("motion_not_found", "Motion not found."));
  }

  private Procedure procedure(UUID constitutionId, String code) {
    return procedures
        .findEntityByConstitutionVersionIdAndCode(constitutionId, code)
        .orElseThrow(() -> ApiException.notFound("procedure_not_found", "Procedure not found."));
  }

  private Procedure procedure(UUID procedureId) {
    return procedures
        .findEntityById(procedureId)
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
    return introduceMotion(
        polityId,
        introducer,
        constitution,
        jurisdiction,
        institution,
        procedure,
        title,
        body,
        template,
        introducingPower,
        Set.of(),
        now);
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
      Set<UUID> recusedMembershipIds,
      OffsetDateTime now) {
    OffsetDateTime votingOpensAt = now.plusHours(procedure.getMinimumNoticeHours());
    OffsetDateTime votingClosesAt = votingOpensAt.plusHours(procedure.getVotingPeriodHours());
    List<Membership> eligible =
        procedureElectorates.electors(procedure, votingOpensAt).stream()
            .filter(member -> !recusedMembershipIds.contains(member.getId()))
            .toList();
    requireEligibleElectorate(eligible);
    requireMinimumElectorate(eligible, procedure);
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
            introducedMotionParams(
                motion,
                title,
                introducer,
                procedure,
                institution,
                constitution,
                votingOpensAt,
                eligible.size())),
        now);
    return motion;
  }

  private void requireEligibleElectorate(List<Membership> eligible) {
    if (eligible.isEmpty()) {
      throw ApiException.conflict(
          "procedure_electorate_empty",
          "This procedure has no eligible electors under the current constitution.");
    }
  }

  private void requireMinimumElectorate(List<Membership> eligible, Procedure procedure) {
    if (eligible.size() < procedure.getMinimumElectorCount()) {
      throw ApiException.conflict(
          "procedure_electorate_below_minimum",
          "This procedure does not have enough eligible electors under the current constitution.");
    }
  }

  private Map<String, Object> introducedMotionParams(
      Motion motion,
      String title,
      Membership introducer,
      Procedure procedure,
      Institution institution,
      ConstitutionVersion constitution,
      OffsetDateTime votingOpensAt,
      int eligibleElectorCount) {
    return TemplateParameters.with(
        motion.getTemplateParams(),
        "motionTitle",
        title,
        "motionTitleKey",
        motion.getTitleKey(),
        "motionBodyKey",
        motion.getBodyKey(),
        "motionTemplateParams",
        motion.getTemplateParams(),
        "introducerName",
        introducer.getDisplayName(),
        "procedureName",
        procedure.getName(),
        "procedureNameKey",
        procedure.getNameKey(),
        "procedureCode",
        procedure.getCode(),
        "institutionName",
        institution.getName(),
        "institutionNameKey",
        institution.getNameKey(),
        "institutionKind",
        institution.getKind().name(),
        "constitutionVersion",
        constitution.getVersion(),
        "votingOpensAt",
        votingOpensAt.toString(),
        "eligibleElectorCount",
        eligibleElectorCount);
  }

  private Membership activeMembership(UUID membershipId, UUID polityId) {
    return memberships
        .findEntityById(membershipId)
        .filter(member -> member.getPolityId().equals(polityId))
        .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
        .orElseThrow(() -> ApiException.notFound("member_not_found", "Member not found."));
  }

  private OfficeTerm reviewableOfficeTerm(UUID officeTermId, UUID polityId, OffsetDateTime now) {
    OfficeTerm term =
        officeTerms
            .findEntityByIdAndPolityId(officeTermId, polityId)
            .orElseThrow(
                () -> ApiException.notFound("office_term_not_found", "Office term not found."));
    if (term.getStatus() != OfficeTermStatus.ACTIVE || !term.getEndsAt().isAfter(now)) {
      throw ApiException.conflict(
          "office_term_not_reviewable", "Only active office terms can be reviewed.");
    }
    return term;
  }

  private Office currentOffice(UUID officeId, UUID polityId, ConstitutionVersion constitution) {
    Office office =
        offices
            .findEntityByIdAndPolityId(officeId, polityId)
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
            .findProjectedByMotionId(motion.getId())
            .map(proposal -> proposal.getAppellantMembershipId().equals(requester.getId()))
            .orElse(false);
  }

  private void disqualifyOfficeElectionCandidatesWithoutStanding(
      UUID motionId, OffsetDateTime now) {
    List<OfficeElectionCandidate> disqualified =
        officeElectionCandidates
            .findEntitiesByMotionIdAndStatus(motionId, OfficeElectionCandidateStatus.ACCEPTED)
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
        .findEntitiesByMotionIdAndStatus(motionId, OfficeElectionCandidateStatus.ACCEPTED)
        .stream()
        .map(
            candidate -> {
              Membership membership =
                  activeMembership(candidate.getMembershipId(), candidate.getPolityId());
              if (requireStanding && !membershipService.hasPoliticalStanding(membership, now)) {
                return null;
              }
              if (requireStanding && officeHeldByCandidate(motionId, membership.getId(), now)) {
                return null;
              }
              return new OfficeElectionCandidateOption(
                  membership.getId(), membership.getDisplayName());
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private boolean officeHeldByCandidate(UUID motionId, UUID membershipId, OffsetDateTime now) {
    return officeElectionProposals
        .findProjectedByMotionId(motionId)
        .map(
            proposal -> {
              Office office =
                  offices
                      .findEntityByIdAndPolityId(proposal.getOfficeId(), proposal.getPolityId())
                      .orElseThrow(
                          () -> ApiException.notFound("office_not_found", "Office not found."));
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

  private void requireAppealable(UUID polityId, Sanction sanction, OffsetDateTime now) {
    if (sanction.isInactiveAt(now)) {
      throw ApiException.conflict("sanction_not_active", "Only active sanctions can be appealed.");
    }
    if (appeals.existsByPolityIdAndSanctionId(polityId, sanction.getId())) {
      throw ApiException.conflict(
          "appeal_already_granted", "This sanction has already been appealed.");
    }
    if (hasOpenAppealProposal(polityId, sanction.getId())) {
      throw ApiException.conflict(
          "appeal_already_open", "This sanction already has an open appeal motion.");
    }
  }

  private boolean hasOpenAppealProposal(UUID polityId, UUID sanctionId) {
    return appealProposals.findProjectionsByPolityIdAndSanctionId(polityId, sanctionId).stream()
        .anyMatch(proposal -> isVotingMotion(proposal.getMotionId()));
  }

  private void requireReviewable(UUID polityId, OfficeTerm term) {
    if (hasOpenOfficeTermReviewProposal(polityId, term.getId())) {
      throw ApiException.conflict(
          "office_term_review_already_open",
          "This office term already has an open office term review motion.");
    }
  }

  private boolean hasOpenOfficeTermReviewProposal(UUID polityId, UUID officeTermId) {
    return officeTermReviewProposals
        .findProjectionsByPolityIdAndOfficeTermId(polityId, officeTermId)
        .stream()
        .anyMatch(proposal -> isVotingMotion(proposal.getMotionId()));
  }

  private OfficialRecordEntry voidableOfficialAct(UUID polityId, UUID recordId) {
    OfficialRecordEntry record =
        officialRecordEntries
            .findEntityByIdAndPolityId(recordId, polityId)
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "official_record_entry_not_found", "Official record entry not found."));
    if (!record.getType().isVoidableByConstitutionalReview()) {
      throw ApiException.conflict(
          "official_act_not_reviewable",
          "This official act does not have a constitutional-review void remedy.");
    }
    return record;
  }

  private void requireConstitutionalReviewVoidRemedy(
      UUID polityId, OfficialRecordEntry record, OffsetDateTime now) {
    if (hasOpenConstitutionalReviewProposal(polityId, record.getId())) {
      throw ApiException.conflict(
          "constitutional_review_already_open",
          "This official act already has an open constitutional review motion.");
    }
    if (constitutionalReviews.existsByPolityIdAndTargetRecordId(polityId, record.getId())) {
      throw ApiException.conflict(
          "constitutional_review_already_granted",
          "This official act has already been constitutionally reviewed.");
    }
    if (!officialActVoidRemedies.hasActiveRemedy(record, now)) {
      throw ApiException.conflict(
          "official_act_void_remedy_unavailable",
          "This official act no longer has an active remedy to void.");
    }
  }

  private void requireSanctionSafeguards(
      Membership introducer,
      ConstitutionVersion constitution,
      Membership target,
      int durationDays,
      OffsetDateTime now) {
    ActionAvailabilityResult availability = polities.sanctionAvailability(introducer, constitution);
    if (!availability.available()) {
      throw ApiException.conflict(
          availability.reason(), "Sanctions require an available appeal procedure.");
    }
    Procedure appealProcedure = procedure(constitution.getId(), Procedure.APPEAL);
    int minimumDurationDays = minimumAppealableSanctionDurationDays(appealProcedure);
    if (durationDays < minimumDurationDays) {
      throw ApiException.badRequest(
          "sanction_duration_too_short",
          "Sanctions must last long enough for an appeal to be completed.",
          Map.of("minimumDurationDays", minimumDurationDays));
    }
    OffsetDateTime appealVotingOpensAt = now.plusHours(appealProcedure.getMinimumNoticeHours());
    List<Membership> eligible =
        procedureElectorates.electors(appealProcedure, appealVotingOpensAt).stream()
            .filter(member -> !member.getId().equals(target.getId()))
            .filter(member -> !member.getId().equals(introducer.getId()))
            .toList();
    if (eligible.size() < appealProcedure.getMinimumElectorCount()) {
      throw ApiException.conflict(
          "appeal_procedure_conflict_recusal_unavailable",
          "Sanctions require an available appeal procedure after conflict recusal.");
    }
  }

  private int minimumAppealableSanctionDurationDays(Procedure appealProcedure) {
    int appealHours =
        appealProcedure.getMinimumNoticeHours() + appealProcedure.getVotingPeriodHours();
    return appealHours == 0 ? 1 : appealHours / 24 + 1;
  }

  private boolean hasOpenConstitutionalReviewProposal(UUID polityId, UUID targetRecordId) {
    return constitutionalReviewProposals
        .findProjectionsByPolityIdAndTargetRecordId(polityId, targetRecordId)
        .stream()
        .anyMatch(proposal -> isVotingMotion(proposal.getMotionId()));
  }

  private boolean isVotingMotion(UUID motionId) {
    return motions.existsByIdAndStatus(motionId, MotionStatus.VOTING);
  }

  private Set<UUID> recusalsForOfficialAct(
      UUID petitionerMembershipId, OfficialRecordEntry record) {
    Set<UUID> recused = recusals(petitionerMembershipId, record.getActorMembershipId());
    if (record.getType() == OfficialRecordType.SANCTION_APPLIED) {
      sanctions
          .findEntityByIdAndPolityId(record.getSourceId(), record.getPolityId())
          .ifPresent(
              sanction -> {
                recused.add(sanction.getTargetMembershipId());
                recused.add(motion(record.getPolityId(), sanction.getMotionId()).getIntroducedBy());
              });
    }
    if (record.getType() == OfficialRecordType.OFFICE_ELECTED) {
      officeTerms
          .findEntityByIdAndPolityId(record.getSourceId(), record.getPolityId())
          .map(OfficeTerm::getMembershipId)
          .ifPresent(recused::add);
    }
    return recused;
  }

  private Set<UUID> recusals(UUID... membershipIds) {
    Set<UUID> recusals = new HashSet<>();
    for (UUID membershipId : membershipIds) {
      if (membershipId != null) {
        recusals.add(membershipId);
      }
    }
    return recusals;
  }

  private List<CreateProcedureChangeInput> requireProposableProcedureChanges(
      CreateConstitutionAmendmentMotionInput input,
      ConstitutionVersion constitution,
      List<CreateInstitutionChangeInput> institutionChanges,
      List<CreateOfficeChangeInput> officeChanges) {
    List<CreateProcedureChangeInput> changes = proposedChanges(input.procedureChanges());
    Map<UUID, InstitutionKind> institutionKinds =
        resultingInstitutionKinds(constitution, institutionChanges);
    if (changes.isEmpty()) {
      validateProcedureInstitutionReferences(constitution, institutionKinds, Map.of());
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
                  requireCompatibleProcedureThreshold(
                      procedure.getEffectType(),
                      proposedOrCurrent(change.threshold(), procedure.getThreshold()));
                  requireCompatibleOfficeElectionMethod(
                      procedure.getEffectType(),
                      proposedOrCurrent(
                          change.officeElectionMethod(), procedure.getOfficeElectionMethod()));
                  UUID institutionId =
                      proposedOrCurrent(change.institutionId(), procedure.getInstitutionId());
                  requireKnownProcedureInstitution(institutionId, institutionKinds.keySet());
                  requireCompatibleProcedureInstitution(
                      procedure.getEffectType(), institutionId, institutionKinds);
                  ProcedureElectorate electorate =
                      proposedOrCurrent(change.electorate(), procedure.getElectorate());
                  String electorateOfficeCode =
                      resultingElectorateOfficeCode(
                          electorate, change.electorateOfficeCode(), procedure);
                  requireKnownElectorateOffice(electorate, electorateOfficeCode, officeCodes);
                  return change;
                })
            .toList();
    validateProcedureInstitutionReferences(
        constitution, institutionKinds, changedProcedureInstitutions(changes));
    validateProcedureOfficeReferences(
        constitution, officeCodes, changedProcedureElectorates(changes));
    return changes;
  }

  private List<CreateInstitutionChangeInput> requireProposableInstitutionChanges(
      CreateConstitutionAmendmentMotionInput input, ConstitutionVersion constitution) {
    List<CreateInstitutionChangeInput> changes = proposedChanges(input.institutionChanges());
    if (changes.isEmpty()) {
      return List.of();
    }
    Map<UUID, Institution> currentInstitutions = currentInstitutions(constitution);
    Set<UUID> jurisdictionIds = currentJurisdictionIds(constitution.getPolityId());
    changes.forEach(
        change -> {
          Institution institution =
              change.institutionId() == null
                  ? null
                  : currentInstitutions.get(change.institutionId());
          switch (change.action()) {
            case CREATE -> requireKnownJurisdiction(change.jurisdictionId(), jurisdictionIds);
            case REVISE -> {
              if (institution == null) {
                throw ApiException.notFound("institution_not_found", "Institution not found.");
              }
              if (change.jurisdictionId() != null) {
                requireKnownJurisdiction(change.jurisdictionId(), jurisdictionIds);
              }
            }
            case RETIRE -> {
              if (institution == null) {
                throw ApiException.notFound("institution_not_found", "Institution not found.");
              }
            }
          }
        });
    return changes;
  }

  private List<CreateOfficeChangeInput> requireProposableOfficeChanges(
      CreateConstitutionAmendmentMotionInput input,
      ConstitutionVersion constitution,
      OffsetDateTime now) {
    List<CreateOfficeChangeInput> changes = proposedChanges(input.officeChanges());
    if (changes.isEmpty()) {
      return List.of();
    }
    Map<String, Office> currentOffices = currentOffices(constitution);
    Set<UUID> jurisdictionIds = currentJurisdictionIds(constitution.getPolityId());
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
              if (change.jurisdictionId() != null) {
                requireKnownJurisdiction(change.jurisdictionId(), jurisdictionIds);
              }
            }
            case REVISE -> {
              if (office == null) {
                throw ApiException.notFound("office_not_found", "Office not found.");
              }
              if (change.jurisdictionId() != null) {
                requireKnownJurisdiction(change.jurisdictionId(), jurisdictionIds);
              }
              if (change.seatCount() != null) {
                requireOfficeSeatCapacity(
                    constitution.getPolityId(), code, change.seatCount(), now);
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
    List<CreatePowerChangeInput> changes = proposedChanges(input.powerChanges());
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
        change.institutionId(),
        change.quorumNumerator(),
        change.quorumDenominator(),
        change.threshold(),
        change.officeElectionMethod(),
        change.electorate(),
        trimmedProposalValue(change.electorateOfficeCode()),
        change.minimumElectorCount(),
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
        change.jurisdictionId(),
        trimmedProposalValue(change.name()),
        trimmedProposalValue(change.description()),
        change.termLengthDays(),
        change.seatCount());
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

  private ConstitutionInstitutionChangeProposal toProposal(
      UUID polityId, UUID amendmentProposalId, CreateInstitutionChangeInput change) {
    return new ConstitutionInstitutionChangeProposal(
        polityId,
        amendmentProposalId,
        change.action(),
        change.institutionId(),
        change.jurisdictionId(),
        trimmedProposalValue(change.name()),
        change.kind());
  }

  private Map<String, Office> currentOffices(ConstitutionVersion constitution) {
    return offices.findEntitiesByConstitutionVersionIdOrderByName(constitution.getId()).stream()
        .collect(java.util.stream.Collectors.toMap(Office::getCode, office -> office));
  }

  private Map<PowerCode, ConstitutionalPower> currentPowers(ConstitutionVersion constitution) {
    return powers.findEntitiesByConstitutionVersionId(constitution.getId()).stream()
        .collect(java.util.stream.Collectors.toMap(ConstitutionalPower::getCode, power -> power));
  }

  private Map<UUID, Institution> currentInstitutions(ConstitutionVersion constitution) {
    return institutions.findEntitiesByConstitutionVersionId(constitution.getId()).stream()
        .collect(java.util.stream.Collectors.toMap(Institution::getId, institution -> institution));
  }

  private Set<UUID> currentJurisdictionIds(UUID polityId) {
    return jurisdictions.findEntitiesByPolityId(polityId).stream()
        .map(Jurisdiction::getId)
        .collect(java.util.stream.Collectors.toSet());
  }

  private Map<UUID, InstitutionKind> resultingInstitutionKinds(
      ConstitutionVersion constitution, List<CreateInstitutionChangeInput> institutionChanges) {
    Map<UUID, InstitutionKind> kinds = new HashMap<>();
    currentInstitutions(constitution)
        .forEach((id, institution) -> kinds.put(id, institution.getKind()));
    institutionChanges.forEach(
        change -> {
          switch (change.action()) {
            case CREATE -> {
              // New institutions cannot be procedure targets in the same amendment.
            }
            case REVISE ->
                kinds.put(
                    change.institutionId(),
                    proposedOrCurrent(change.kind(), kinds.get(change.institutionId())));
            case RETIRE -> kinds.remove(change.institutionId());
          }
        });
    return kinds;
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
          "citizen_power_required", "Disbandment must remain an active citizen power.");
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
        .findEntitiesByConstitutionVersionId(constitution.getId())
        .forEach(
            procedure -> {
              CreateProcedureChangeInput change = procedureChanges.get(procedure.getCode());
              ProcedureElectorate electorate =
                  change == null
                      ? procedure.getElectorate()
                      : proposedOrCurrent(change.electorate(), procedure.getElectorate());
              String electorateOfficeCode =
                  change == null
                      ? procedure.getElectorateOfficeCode()
                      : resultingElectorateOfficeCode(
                          electorate, change.electorateOfficeCode(), procedure);
              requireKnownElectorateOffice(electorate, electorateOfficeCode, officeCodes);
            });
  }

  private void validateProcedureInstitutionReferences(
      ConstitutionVersion constitution,
      Map<UUID, InstitutionKind> institutionKinds,
      Map<String, CreateProcedureChangeInput> procedureChanges) {
    procedures
        .findEntitiesByConstitutionVersionId(constitution.getId())
        .forEach(
            procedure -> {
              CreateProcedureChangeInput change = procedureChanges.get(procedure.getCode());
              UUID institutionId =
                  change == null
                      ? procedure.getInstitutionId()
                      : proposedOrCurrent(change.institutionId(), procedure.getInstitutionId());
              requireKnownProcedureInstitution(institutionId, institutionKinds.keySet());
              requireCompatibleProcedureInstitution(
                  procedure.getEffectType(), institutionId, institutionKinds);
            });
  }

  private void requireKnownProcedureInstitution(UUID institutionId, Set<UUID> institutionIds) {
    if (!institutionIds.contains(institutionId)) {
      throw ApiException.badRequest(
          "procedure_institution_missing",
          "Procedures must refer to an institution in the amended constitution.");
    }
  }

  private void requireCompatibleProcedureInstitution(
      EffectType effectType, UUID institutionId, Map<UUID, InstitutionKind> institutionKinds) {
    InstitutionKind institutionKind = institutionKinds.get(institutionId);
    if (!effectType.supportsInstitutionKind(institutionKind)) {
      throw ApiException.badRequest(
          "procedure_institution_kind_mismatch",
          "Procedures must belong to an institution kind compatible with their effect.");
    }
  }

  private void requireCompatibleProcedureThreshold(
      EffectType effectType, VotingThreshold threshold) {
    if (effectType == EffectType.ELECT_OFFICE
        && threshold != VotingThreshold.OFFICE_ELECTION_RESULT) {
      throw ApiException.badRequest(
          "office_election_result_threshold_required",
          "Office election procedures must use office-election result thresholds.");
    }
    if (effectType != EffectType.ELECT_OFFICE
        && threshold == VotingThreshold.OFFICE_ELECTION_RESULT) {
      throw ApiException.badRequest(
          "office_election_result_threshold_requires_election",
          "Office-election result thresholds can only be used by office election procedures.");
    }
  }

  private void requireCompatibleOfficeElectionMethod(
      EffectType effectType, OfficeElectionMethod officeElectionMethod) {
    if (effectType == EffectType.ELECT_OFFICE && officeElectionMethod == null) {
      throw ApiException.badRequest(
          "office_election_method_required",
          "Office election procedures must define an office election method.");
    }
    if (effectType != EffectType.ELECT_OFFICE && officeElectionMethod != null) {
      throw ApiException.badRequest(
          "office_election_method_requires_election",
          "Office election methods can only be used by office election procedures.");
    }
  }

  private void requireKnownJurisdiction(UUID jurisdictionId, Set<UUID> jurisdictionIds) {
    if (!jurisdictionIds.contains(jurisdictionId)) {
      throw ApiException.badRequest(
          "constitution_change_jurisdiction_not_found",
          "Constitution changes must refer to an existing jurisdiction.");
    }
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

  private String resultingElectorateOfficeCode(
      ProcedureElectorate electorate, String proposedOfficeCode, Procedure currentProcedure) {
    if (electorate != ProcedureElectorate.OFFICE_HOLDERS) {
      return null;
    }
    return proposedOrCurrent(
        normalizedProposalOfficeCode(proposedOfficeCode),
        currentProcedure.getElectorateOfficeCode());
  }

  private void requireOfficeSeatCapacity(
      UUID polityId, String officeCode, int seatCount, OffsetDateTime now) {
    if (officeTerms.countByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            polityId, officeCode, OfficeTermStatus.ACTIVE, now)
        > seatCount) {
      throw ApiException.badRequest(
          "office_seat_capacity_insufficient",
          "Office seat count cannot be reduced below the current active terms.");
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

  private Map<String, CreateProcedureChangeInput> changedProcedureInstitutions(
      List<CreateProcedureChangeInput> procedureChanges) {
    Map<String, CreateProcedureChangeInput> changes = new java.util.HashMap<>();
    procedureChanges.forEach(change -> changes.put(change.procedureCode(), change));
    return changes;
  }

  private <T> T proposedOrCurrent(T value, T fallback) {
    return value == null ? fallback : value;
  }

  private <T> List<T> proposedChanges(List<T> values) {
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
