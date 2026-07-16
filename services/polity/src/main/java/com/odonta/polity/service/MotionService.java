package com.odonta.polity.service;

import com.odonta.authorization.spring.AuthenticatedUser;
import com.odonta.common.api.ApiException;
import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.authorization.PolityAccessPolicy;
import com.odonta.polity.effect.MotionEffectApplier;
import com.odonta.polity.effect.OfficialActVoidRemedy;
import com.odonta.polity.evaluator.ConstitutionAmendmentEvaluationException;
import com.odonta.polity.evaluator.ConstitutionAmendmentEvaluator;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.evaluator.VotingEvaluator;
import com.odonta.polity.exception.PolityResource;
import com.odonta.polity.input.CastOfficeElectionBallotInput;
import com.odonta.polity.input.CastVoteInput;
import com.odonta.polity.input.CreateAppealMotionInput;
import com.odonta.polity.input.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.input.CreateConstitutionalReviewMotionInput;
import com.odonta.polity.input.CreateDisbandmentMotionInput;
import com.odonta.polity.input.CreateInstitutionChangeInput;
import com.odonta.polity.input.CreateMotionInput;
import com.odonta.polity.input.CreateOfficeChangeInput;
import com.odonta.polity.input.CreateOfficeElectionMotionInput;
import com.odonta.polity.input.CreateOfficeTermReviewMotionInput;
import com.odonta.polity.input.CreatePowerChangeInput;
import com.odonta.polity.input.CreateProcedureChangeInput;
import com.odonta.polity.input.CreateSanctionMotionInput;
import com.odonta.polity.input.RespondOfficeElectionCandidacyInput;
import com.odonta.polity.input.ValidatedConstitutionAmendmentInput;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.Certification;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionInstitutionChangeProposal;
import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionStatus;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.ConstitutionalReviewProposal;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionElector;
import com.odonta.polity.model.MotionStatus;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.MotionTemplateKey;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionBallot;
import com.odonta.polity.model.OfficeElectionBallotPreference;
import com.odonta.polity.model.OfficeElectionBallotRanking;
import com.odonta.polity.model.OfficeElectionCandidate;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.OfficeElectionProposal;
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
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionProposal;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.model.Vote;
import com.odonta.polity.model.VoteChoice;
import com.odonta.polity.model.VotingResult;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.CertificationRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalReviewProposalRepository;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
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
import com.odonta.polity.resolver.ConstitutionAmendmentStateResolver;
import com.odonta.polity.resolver.MotionResultResolver;
import com.odonta.polity.resolver.PolityActionAvailabilityResolver;
import com.odonta.polity.resolver.ProcedureElectorateResolver;
import com.odonta.polity.result.ActionAvailabilityResult;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.result.PageResult;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
  private final ConstitutionAmendmentEvaluator amendmentEvaluator;
  private final ConstitutionAmendmentProposalRepository amendmentProposals;
  private final ConstitutionAmendmentStateResolver amendmentStates;
  private final ConstitutionInstitutionChangeProposalRepository institutionChangeProposals;
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals;
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals;
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalReviewProposalRepository constitutionalReviewProposals;
  private final ConstitutionalReviewRepository constitutionalReviews;
  private final MotionEffectApplier effects;
  private final OfficeTermReviewProposalRepository officeTermReviewProposals;
  private final MotionElectorRepository electors;
  private final MembershipService membershipService;
  private final MembershipRepository memberships;
  private final MotionResultResolver results;
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
  private final PolityActionAvailabilityResolver actionAvailability;
  private final ProcedureElectorateResolver procedureElectorates;
  private final ProcedureRepository procedures;
  private final SanctionProposalRepository sanctionProposals;
  private final SanctionRepository sanctions;
  private final VoteRepository votes;
  private final VotingEvaluator voting;

  // region Operations: Introduction

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult create(
      UUID polityId, AuthenticatedUser actor, @Valid CreateMotionInput input) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    polities.requireActive(polityId);
    Membership introducer = membershipService.active(polityId, actor.id());
    ConstitutionVersion constitution = polities.constitution(polityId);
    ConstitutionalMotionPath path = ConstitutionalMotionPath.ORDINARY_GOVERNANCE;
    authority.require(introducer, constitution, path.introducingPower());
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), path);
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
            path,
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
    ConstitutionalMotionPath path = ConstitutionalMotionPath.OFFICE_ELECTION;
    authority.require(introducer, constitution, path.introducingPower());
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), path);
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
            path,
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
    ConstitutionalMotionPath path = ConstitutionalMotionPath.SANCTION;
    authority.require(introducer, constitution, path.introducingPower());
    Membership target = activeMembership(input.targetMembershipId(), polityId);
    requireSanctionSafeguards(introducer, constitution, target, input.durationDays(), now);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), path);
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
            path,
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
    ConstitutionalMotionPath path = ConstitutionalMotionPath.APPEAL;
    Sanction sanction =
        sanctions
            .findEntityByIdAndPolityId(input.sanctionId(), polityId)
            .orElseThrow(PolityResource.SANCTION::notFound);
    requireAppealable(polityId, sanction, now);
    if (sanction.getTargetMembershipId().equals(introducer.getId())) {
      authority.requireOwnAppealIntroduction(introducer, constitution);
    } else {
      authority.require(introducer, constitution, path.introducingPower());
    }
    UUID sanctionIntroducerId = motion(polityId, sanction.getMotionId()).getIntroducedBy();
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), path);
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
            path,
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
    ConstitutionalMotionPath path = ConstitutionalMotionPath.OFFICE_TERM_REVIEW;
    authority.require(introducer, constitution, path.introducingPower());
    OfficeTerm term = reviewableOfficeTerm(input.officeTermId(), polityId, now);
    requireReviewable(polityId, term);
    Office office =
        offices
            .findEntityByIdAndPolityId(term.getOfficeId(), polityId)
            .orElseThrow(PolityResource.OFFICE::notFound);
    Membership officeHolder = membershipService.get(term.getMembershipId());
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), path);
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
            path,
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
    ConstitutionalMotionPath path = ConstitutionalMotionPath.CONSTITUTIONAL_REVIEW;
    authority.require(introducer, constitution, path.introducingPower());
    OfficialRecordEntry target = voidableOfficialAct(polityId, input.targetRecordId());
    requireConstitutionalReviewVoidRemedy(polityId, target, now);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), path);
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
            path,
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
    ConstitutionalMotionPath path = ConstitutionalMotionPath.CONSTITUTION_AMENDMENT;
    authority.require(introducer, constitution, path.introducingPower());
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), path);
    Institution institution = polities.institution(polityId, procedure);
    ValidatedConstitutionAmendmentInput plan;
    try {
      plan = amendmentEvaluator.evaluate(amendmentStates.resolve(constitution, now), input);
    } catch (ConstitutionAmendmentEvaluationException exception) {
      throw switch (exception.kind()) {
        case INVALID -> ApiException.badRequest(exception.code(), exception.getMessage());
        case MISSING_REFERENCE -> ApiException.notFound(exception.code(), exception.getMessage());
        case CONFLICTING_STATE -> ApiException.conflict(exception.code(), exception.getMessage());
      };
    }
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
            path,
            now);
    ConstitutionAmendmentProposal proposal =
        amendmentProposals.saveAndFlush(
            new ConstitutionAmendmentProposal(
                polityId, motion.getId(), input.title(), input.body()));
    institutionChangeProposals.saveAllAndFlush(
        plan.institutionChanges().stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    procedureChangeProposals.saveAllAndFlush(
        plan.procedureChanges().stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    officeChangeProposals.saveAllAndFlush(
        plan.officeChanges().stream()
            .map(change -> toProposal(polityId, proposal.getId(), change))
            .toList());
    powerChangeProposals.saveAllAndFlush(
        plan.powerChanges().stream()
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
    ConstitutionalMotionPath path = ConstitutionalMotionPath.DISBANDMENT;
    authority.require(introducer, constitution, path.introducingPower());
    polities.requireDisbandmentGovernment(polityId);
    Jurisdiction jurisdiction = polities.jurisdiction(polityId);
    Procedure procedure = procedure(constitution.getId(), path);
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
            path,
            now);
    return result(motion);
  }

  // endregion

  // region Operations: Queries

  @Transactional(readOnly = true)
  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public PageResult<MotionResult> list(UUID polityId, UUID userId, int page, int size) {
    access.requireReadable(polityId, userId);
    UUID currentMembershipId = currentMembershipId(polityId, userId);
    Page<MotionProjection> projections =
        motions.findProjectionsByPolityIdOrderByOpenedAtDescIdAsc(
            polityId, PageRequest.of(page, size));
    return new PageResult<>(
        results.resolveAll(polityId, projections.getContent(), currentMembershipId),
        projections.getNumber(),
        projections.getSize(),
        projections.getTotalElements());
  }

  @Transactional(readOnly = true)
  @PreAuthorize(PolityPermissions.CAN_READ_POLITY)
  public MotionResult get(UUID polityId, UUID motionId, UUID userId) {
    access.requireReadable(polityId, userId);
    return results.resolve(polityId, motionId, currentMembershipId(polityId, userId));
  }

  // endregion

  // region Operations: Participation

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

  // endregion

  // region Operations: Certification

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
            .orElseThrow(PolityResource.CONSTITUTION::notFound);
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
            .orElseThrow(PolityResource.PROCEDURE::notFound);
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

  // endregion

  // region Mechanics: Results and elections

  private MotionResult result(Motion motion) {
    return results.resolve(motion, motion.getIntroducedBy());
  }

  private MotionResult result(Motion motion, UUID currentMembershipId) {
    return results.resolve(motion, currentMembershipId);
  }

  private UUID currentMembershipId(UUID polityId, UUID userId) {
    return memberships
        .findEntityByPolityIdAndUserIdAndStatus(polityId, userId, MembershipStatus.ACTIVE)
        .map(Membership::getId)
        .orElse(null);
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

  // endregion

  // region Mechanics: Lifecycle

  private Motion motion(UUID polityId, UUID motionId) {
    return motions
        .findEntityByIdAndPolityId(motionId, polityId)
        .orElseThrow(PolityResource.MOTION::notFound);
  }

  private Procedure procedure(UUID constitutionId, String code) {
    return procedures
        .findEntityByConstitutionVersionIdAndCode(constitutionId, code)
        .orElseThrow(PolityResource.PROCEDURE::notFound);
  }

  private Procedure procedure(UUID constitutionId, ConstitutionalMotionPath path) {
    return procedure(constitutionId, path.procedureCode());
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
      ConstitutionalMotionPath path,
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
        path,
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
      ConstitutionalMotionPath path,
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
        OfficialRecordContext.motion(
            motion, path.introducingPower(), OfficialRecordOutcome.INTRODUCED),
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

  // endregion

  // region Mechanics: Eligibility

  private Membership activeMembership(UUID membershipId, UUID polityId) {
    return memberships
        .findEntityById(membershipId)
        .filter(member -> member.getPolityId().equals(polityId))
        .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
        .orElseThrow(PolityResource.MEMBER::notFound);
  }

  private OfficeTerm reviewableOfficeTerm(UUID officeTermId, UUID polityId, OffsetDateTime now) {
    OfficeTerm term =
        officeTerms
            .findEntityByIdAndPolityId(officeTermId, polityId)
            .orElseThrow(PolityResource.OFFICE_TERM::notFound);
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
            .orElseThrow(PolityResource.OFFICE::notFound);
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

  // endregion

  // region Mechanics: Constitutional safeguards

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
            .orElseThrow(PolityResource.OFFICIAL_RECORD_ENTRY::notFound);
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
    ActionAvailabilityResult availability =
        actionAvailability.sanctionAvailability(introducer, constitution);
    if (!availability.available()) {
      throw ApiException.conflict(
          availability.reason().wireValue(), "Sanctions require an available appeal procedure.");
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

  // endregion

  // region Materialization: Amendments

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
        change.electorateOfficeCode(),
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
        change.code(),
        change.jurisdictionId(),
        change.name(),
        change.description(),
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
        change.holderOfficeCode());
  }

  private ConstitutionInstitutionChangeProposal toProposal(
      UUID polityId, UUID amendmentProposalId, CreateInstitutionChangeInput change) {
    return new ConstitutionInstitutionChangeProposal(
        polityId,
        amendmentProposalId,
        change.action(),
        change.institutionId(),
        change.jurisdictionId(),
        change.name(),
        change.kind());
  }

  // endregion
}
