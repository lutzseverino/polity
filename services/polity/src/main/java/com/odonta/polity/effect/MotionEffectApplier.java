package com.odonta.polity.effect;

import com.odonta.authorization.grant.Revocations;
import com.odonta.common.api.ApiException;
import com.odonta.polity.authorization.PolityRevocationPlanner;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.model.Appeal;
import com.odonta.polity.model.ConstitutionChangeItem;
import com.odonta.polity.model.ConstitutionChangeKind;
import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.model.ConstitutionInstitutionChangeAction;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.ConstitutionalReview;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.MembershipStatus;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeElectionCandidateOption;
import com.odonta.polity.model.OfficeElectionCandidateStatus;
import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.model.OfficeTerm;
import com.odonta.polity.model.OfficeTermReview;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.Resolution;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.AppealProposalProjection;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalProjection;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionInstitutionChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalProjection;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.ConstitutionalReviewProposalProjection;
import com.odonta.polity.repository.ConstitutionalReviewProposalRepository;
import com.odonta.polity.repository.ConstitutionalReviewRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.OfficeTermReviewProposalProjection;
import com.odonta.polity.repository.OfficeTermReviewProposalRepository;
import com.odonta.polity.repository.OfficeTermReviewRepository;
import com.odonta.polity.repository.OfficialRecordRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.SanctionProposalProjection;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import com.odonta.polity.service.MembershipService;
import com.odonta.polity.service.OfficialRecordService;
import java.time.OffsetDateTime;
import java.util.HashMap;
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
public class MotionEffectApplier {
  private final AppealProposalRepository appealProposals;
  private final AppealRepository appeals;
  private final ConstitutionAmendmentProposalRepository amendmentProposals;
  private final ConstitutionInstitutionChangeProposalRepository institutionChangeProposals;
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals;
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals;
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final ConstitutionalReviewProposalRepository constitutionalReviewProposals;
  private final ConstitutionalReviewRepository constitutionalReviews;
  private final InstitutionRepository institutions;
  private final JurisdictionRepository jurisdictions;
  private final OfficeTermReviewProposalRepository officeTermReviewProposals;
  private final OfficeTermReviewRepository officeTermReviews;
  private final MotionElectorRepository electors;
  private final OfficeElectionBallotRepository officeElectionBallots;
  private final OfficeElectionCandidateRepository officeElectionCandidates;
  private final OfficeElectionEvaluator officeElections;
  private final OfficeElectionProposalRepository officeElectionProposals;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficialActVoidRemedy officialActVoidRemedies;
  private final OfficialRecordRepository officialRecordEntries;
  private final PolityRepository polities;
  private final OfficialRecordService officialRecords;
  private final ProcedureRepository procedures;
  private final ResolutionRepository resolutions;
  private final SanctionProposalRepository sanctionProposals;
  private final SanctionRepository sanctions;
  private final MembershipRepository memberships;
  private final MembershipService membershipService;
  private final PolityRevocationPlanner revocationPlanner;
  private final Revocations revocations;

  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    switch (motion.getEffectType()) {
      case ADOPT_RESOLUTION -> applyResolution(motion, actor, constitution, now);
      case ELECT_OFFICE -> electOffice(motion, actor, constitution, now);
      case APPLY_SANCTION -> applySanction(motion, actor, constitution, now);
      case GRANT_APPEAL -> grantAppeal(motion, actor, constitution, now);
      case VACATE_OFFICE_TERM -> vacateOfficeTerm(motion, actor, constitution, now);
      case VOID_OFFICIAL_ACT -> voidOfficialAct(motion, actor, constitution, now);
      case AMEND_CONSTITUTION -> amendConstitution(motion, actor, constitution, now);
      case DISBAND_POLITY -> disbandPolity(motion, actor, constitution, now);
    }
  }

  private void applyResolution(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    Resolution resolution =
        resolutions.saveAndFlush(
            new Resolution(
                motion.getPolityId(), motion.getId(), motion.getTitle(), motion.getBody(), now));
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.RESOLUTION_ADOPTED,
        resolution.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.ADOPTED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.RESOLUTION_ADOPTED,
            TemplateParameters.with(
                motion.getTemplateParams(),
                "motionTitle",
                motion.getTitle(),
                "motionTitleKey",
                motion.getTitleKey(),
                "motionBody",
                motion.getBody(),
                "motionBodyKey",
                motion.getBodyKey())),
        now);
  }

  private void electOffice(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    var proposal =
        officeElectionProposals
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
    OfficeElectionTallyResult result =
        officeElections.evaluate(
            procedure,
            Math.toIntExact(electors.countByMotionId(motion.getId())),
            electionCandidates(motion.getId(), now),
            officeElectionBallots.findEntitiesByMotionId(motion.getId()));
    if (!result.passed() || result.winnerMembershipId() == null) {
      throw ApiException.conflict(
          "office_election_not_passed", "Only passed office elections can assign office terms.");
    }
    if (officeTerms.existsByPolityIdAndOfficeCodeAndMembershipIdAndStatusAndEndsAtAfter(
        motion.getPolityId(),
        office.getCode(),
        result.winnerMembershipId(),
        OfficeTermStatus.ACTIVE,
        now)) {
      throw ApiException.conflict(
          "office_term_already_held",
          "The elected member already holds an active term for this office.");
    }
    if (officeTerms.countByPolityIdAndOfficeCodeAndStatusAndEndsAtAfter(
            motion.getPolityId(), office.getCode(), OfficeTermStatus.ACTIVE, now)
        >= office.getSeatCount()) {
      throw ApiException.conflict(
          "office_seats_full", "This office has no vacant seats for another active term.");
    }
    OfficeTerm term =
        officeTerms.saveAndFlush(
            new OfficeTerm(
                motion.getPolityId(),
                office.getId(),
                office.getCode(),
                result.winnerMembershipId(),
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
                result.winnerName(),
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

  private void applySanction(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    SanctionProposalProjection proposal =
        sanctionProposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "sanction_proposal_not_found", "Sanction proposal not found."));
    Sanction sanction =
        sanctions.saveAndFlush(
            new Sanction(
                motion.getPolityId(),
                motion.getId(),
                proposal.getTargetMembershipId(),
                proposal.getType(),
                proposal.getReason(),
                now,
                now.plusDays(proposal.getDurationDays())));
    Membership target = membership(proposal.getTargetMembershipId());
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.SANCTION_APPLIED,
        sanction.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.SANCTION_APPLIED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.SANCTION_APPLIED,
            TemplateParameters.of(
                "memberName",
                target.getDisplayName(),
                "sanctionType",
                proposal.getType().name(),
                "sanctionTypeKey",
                proposal.getType().labelKey(),
                "durationDays",
                proposal.getDurationDays(),
                "reason",
                proposal.getReason())),
        now);
  }

  private void grantAppeal(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    AppealProposalProjection proposal =
        appealProposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "appeal_proposal_not_found", "Appeal proposal not found."));
    Sanction sanction =
        sanctions
            .findEntityByIdAndPolityId(proposal.getSanctionId(), motion.getPolityId())
            .orElseThrow(() -> ApiException.notFound("sanction_not_found", "Sanction not found."));
    if (sanction.isInactiveAt(now)) {
      throw ApiException.conflict("sanction_not_active", "Only active sanctions can be appealed.");
    }
    if (appeals.existsByPolityIdAndSanctionId(motion.getPolityId(), sanction.getId())) {
      throw ApiException.conflict(
          "appeal_already_granted", "This sanction has already been appealed.");
    }
    sanction.vacate(now);
    sanctions.saveAndFlush(sanction);
    Appeal appeal =
        appeals.saveAndFlush(
            new Appeal(
                motion.getPolityId(),
                motion.getId(),
                sanction.getId(),
                proposal.getAppellantMembershipId(),
                proposal.getReason(),
                now));
    Membership appellant = membership(proposal.getAppellantMembershipId());
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.APPEAL_GRANTED,
        appeal.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.APPEAL_GRANTED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.APPEAL_GRANTED,
            TemplateParameters.of(
                "memberName", appellant.getDisplayName(), "reason", proposal.getReason())),
        now);
  }

  private void vacateOfficeTerm(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    OfficeTermReviewProposalProjection proposal =
        officeTermReviewProposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "office_term_review_proposal_not_found",
                        "Office term review proposal not found."));
    OfficeTerm term =
        officeTerms
            .findEntityByIdAndPolityId(proposal.getOfficeTermId(), motion.getPolityId())
            .orElseThrow(
                () -> ApiException.notFound("office_term_not_found", "Office term not found."));
    if (term.getStatus() != OfficeTermStatus.ACTIVE || !term.getEndsAt().isAfter(now)) {
      throw ApiException.conflict(
          "office_term_not_vacatable", "Only active office terms can be vacated.");
    }
    if (officeTermReviews.existsByPolityIdAndOfficeTermId(motion.getPolityId(), term.getId())) {
      throw ApiException.conflict(
          "office_term_review_already_granted", "This office term has already been reviewed.");
    }
    term.end(now);
    officeTerms.saveAndFlush(term);
    OfficeTermReview review =
        officeTermReviews.saveAndFlush(
            new OfficeTermReview(
                motion.getPolityId(),
                motion.getId(),
                term.getId(),
                proposal.getPetitionerMembershipId(),
                proposal.getReason(),
                now));
    Office office =
        offices
            .findEntityByIdAndPolityId(term.getOfficeId(), motion.getPolityId())
            .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
    Membership holder = membership(term.getMembershipId());
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.OFFICE_TERM_VACATED,
        review.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.OFFICE_TERM_VACATED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.OFFICE_TERM_VACATED,
            TemplateParameters.of(
                "memberName",
                holder.getDisplayName(),
                "officeName",
                office.getName(),
                "officeNameKey",
                office.getNameKey(),
                "officeCode",
                office.getCode(),
                "reason",
                proposal.getReason())),
        now);
  }

  private void voidOfficialAct(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    ConstitutionalReviewProposalProjection proposal =
        constitutionalReviewProposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "constitutional_review_proposal_not_found",
                        "Constitutional review proposal not found."));
    OfficialRecordEntry target =
        officialRecordEntries
            .findEntityByIdAndPolityId(proposal.getTargetRecordId(), motion.getPolityId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "official_record_entry_not_found", "Official record entry not found."));
    if (!target.getType().isVoidableByConstitutionalReview()) {
      throw ApiException.conflict(
          "official_act_not_voidable",
          "This official act does not have a constitutional-review void remedy.");
    }
    if (constitutionalReviews.existsByPolityIdAndTargetRecordId(
        motion.getPolityId(), target.getId())) {
      throw ApiException.conflict(
          "constitutional_review_already_granted",
          "This official act has already been constitutionally reviewed.");
    }
    if (!officialActVoidRemedies.apply(target, now)) {
      throw ApiException.conflict(
          "official_act_void_remedy_unavailable",
          "This official act no longer has an active remedy to void.");
    }
    ConstitutionalReview review =
        constitutionalReviews.saveAndFlush(
            new ConstitutionalReview(
                motion.getPolityId(),
                motion.getId(),
                target.getId(),
                proposal.getPetitionerMembershipId(),
                proposal.getReason(),
                now));
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.OFFICIAL_ACT_VOIDED,
        review.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.VOIDED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.OFFICIAL_ACT_VOIDED,
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
                proposal.getReason())),
        now);
  }

  private void amendConstitution(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    ConstitutionAmendmentProposalProjection proposal =
        amendmentProposals
            .findProjectedByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "amendment_proposal_not_found", "Amendment proposal not found."));
    ConstitutionVersion current =
        constitutions
            .findEntityById(constitution.getId())
            .orElseThrow(
                () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
    Map<String, ConstitutionProcedureChangeProposalProjection> procedureChanges =
        procedureChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ConstitutionProcedureChangeProposalProjection::getProcedureCode,
                    change -> change));
    List<ConstitutionInstitutionChangeProposalProjection> institutionChanges =
        institutionChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId());
    List<ConstitutionOfficeChangeProposalProjection> officeChanges =
        officeChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId());
    Map<PowerCode, ConstitutionPowerChangeProposalProjection> powerChanges =
        powerChangeProposals.findProjectionsByAmendmentProposalId(proposal.getId()).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ConstitutionPowerChangeProposalProjection::getPowerCode, change -> change));
    if (institutionChanges.isEmpty()
        && procedureChanges.isEmpty()
        && officeChanges.isEmpty()
        && powerChanges.isEmpty()) {
      throw ApiException.conflict(
          "amendment_has_no_effect",
          "Constitution amendments must include at least one enforceable change.");
    }
    current.supersede();
    constitutions.saveAndFlush(current);
    ConstitutionVersion amended =
        constitutions.saveAndFlush(
            new ConstitutionVersion(
                motion.getPolityId(),
                current.getVersion() + 1,
                ConstitutionTemplateKey.STRUCTURED_CHARTER.storedTitle(current.getVersion() + 1),
                ConstitutionTemplateKey.STRUCTURED_CHARTER.storedBody(),
                ConstitutionTemplateKey.STRUCTURED_CHARTER,
                now));
    Map<UUID, InstitutionKind> institutionKinds =
        resultingInstitutionKinds(current, institutionChanges);
    Map<UUID, UUID> institutionIds = copyInstitutions(current, amended, institutionChanges);
    copyProcedures(current, amended, institutionIds, institutionKinds, procedureChanges);
    Set<String> officeCodes = copyOffices(current, amended, officeChanges, now);
    copyPowers(current, amended, powerChanges, officeCodes);
    List<Map<String, Object>> changeItems =
        changeItems(current, institutionChanges, procedureChanges, officeChanges, powerChanges)
            .stream()
            .map(ConstitutionChangeItem::toTemplateParameter)
            .toList();
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        current.getId(),
        actor.getId(),
        OfficialRecordType.CONSTITUTION_AMENDED,
        amended.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.CONSTITUTION_AMENDED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.CONSTITUTION_AMENDED,
            TemplateParameters.of(
                "constitutionVersion",
                amended.getVersion(),
                "amendmentBody",
                proposal.getBody(),
                "changeCount",
                changeItems.size(),
                "changeItems",
                changeItems)),
        now);
  }

  private List<ConstitutionChangeItem> changeItems(
      ConstitutionVersion current,
      List<ConstitutionInstitutionChangeProposalProjection> institutionChanges,
      Map<String, ConstitutionProcedureChangeProposalProjection> procedureChanges,
      List<ConstitutionOfficeChangeProposalProjection> officeChanges,
      Map<PowerCode, ConstitutionPowerChangeProposalProjection> powerChanges) {
    Map<UUID, Institution> currentInstitutions =
        institutions.findEntitiesByConstitutionVersionId(current.getId()).stream()
            .collect(Collectors.toMap(Institution::getId, Function.identity()));
    Map<UUID, String> jurisdictionsById =
        jurisdictions.findEntitiesByPolityId(current.getPolityId()).stream()
            .collect(
                Collectors.toMap(
                    com.odonta.polity.model.Jurisdiction::getId,
                    com.odonta.polity.model.Jurisdiction::getName));
    Map<String, Procedure> currentProcedures =
        procedures.findEntitiesByConstitutionVersionId(current.getId()).stream()
            .collect(Collectors.toMap(Procedure::getCode, Function.identity()));
    Map<String, Office> currentOffices =
        offices.findEntitiesByConstitutionVersionIdOrderByName(current.getId()).stream()
            .collect(Collectors.toMap(Office::getCode, Function.identity()));
    Map<PowerCode, ConstitutionalPower> currentPowers =
        powers.findEntitiesByConstitutionVersionId(current.getId()).stream()
            .collect(Collectors.toMap(ConstitutionalPower::getCode, Function.identity()));
    return java.util.stream.Stream.of(
            institutionChanges.stream()
                .map(change -> changeItem(change, currentInstitutions, jurisdictionsById)),
            procedureChanges.values().stream()
                .map(
                    change ->
                        changeItem(change, currentProcedures, currentInstitutions, currentOffices)),
            officeChanges.stream()
                .map(change -> changeItem(change, currentOffices, jurisdictionsById)),
            powerChanges.values().stream()
                .map(change -> changeItem(change, currentPowers, currentOffices)))
        .flatMap(stream -> stream)
        .toList();
  }

  private ConstitutionChangeItem changeItem(
      ConstitutionInstitutionChangeProposalProjection change,
      Map<UUID, Institution> currentInstitutions,
      Map<UUID, String> jurisdictionsById) {
    Institution institution = currentInstitutions.get(change.getInstitutionId());
    return new ConstitutionChangeItem(
        ConstitutionChangeKind.INSTITUTION,
        changeOperation(change.getAction()),
        valueOr(change.getName(), institution == null ? "institution" : institution.getName()),
        change.getName() == null && institution != null ? institution.getNameKey() : null,
        TemplateParameters.ofPresent(
            "jurisdiction",
            jurisdictionName(change.getJurisdictionId(), jurisdictionsById),
            "name",
            change.getName(),
            "kind",
            change.getKind()));
  }

  private ConstitutionChangeItem changeItem(
      ConstitutionProcedureChangeProposalProjection change,
      Map<String, Procedure> currentProcedures,
      Map<UUID, Institution> currentInstitutions,
      Map<String, Office> currentOffices) {
    Procedure procedure = currentProcedures.get(change.getProcedureCode());
    return new ConstitutionChangeItem(
        ConstitutionChangeKind.PROCEDURE,
        ConstitutionChangeOperation.REVISE,
        procedure == null ? change.getProcedureCode() : procedure.getName(),
        procedure == null ? null : procedure.getNameKey(),
        TemplateParameters.ofPresent(
            "institution",
            institutionName(change.getInstitutionId(), currentInstitutions),
            "quorumNumerator",
            change.getQuorumNumerator(),
            "quorumDenominator",
            change.getQuorumDenominator(),
            "threshold",
            change.getThreshold(),
            "electorate",
            change.getElectorate(),
            "electorateOfficeCode",
            officeName(change.getElectorateOfficeCode(), currentOffices),
            "minimumElectorCount",
            change.getMinimumElectorCount(),
            "minimumNoticeHours",
            change.getMinimumNoticeHours(),
            "votingPeriodHours",
            change.getVotingPeriodHours()));
  }

  private ConstitutionChangeItem changeItem(
      ConstitutionOfficeChangeProposalProjection change,
      Map<String, Office> currentOffices,
      Map<UUID, String> jurisdictionsById) {
    Office office = currentOffices.get(change.getOfficeCode());
    return new ConstitutionChangeItem(
        ConstitutionChangeKind.OFFICE,
        changeOperation(change.getAction()),
        valueOr(change.getName(), office == null ? change.getOfficeCode() : office.getName()),
        change.getName() == null && office != null ? office.getNameKey() : null,
        TemplateParameters.ofPresent(
            "jurisdiction",
            jurisdictionName(change.getJurisdictionId(), jurisdictionsById),
            "name",
            change.getName(),
            "description",
            change.getDescription(),
            "termLengthDays",
            change.getTermLengthDays(),
            "seatCount",
            change.getSeatCount()));
  }

  private ConstitutionChangeItem changeItem(
      ConstitutionPowerChangeProposalProjection change,
      Map<PowerCode, ConstitutionalPower> currentPowers,
      Map<String, Office> currentOffices) {
    ConstitutionalPower power = currentPowers.get(change.getPowerCode());
    return new ConstitutionChangeItem(
        ConstitutionChangeKind.POWER,
        ConstitutionChangeOperation.REVISE,
        power == null ? change.getPowerCode().name() : power.getName(),
        power == null ? null : power.getNameKey(),
        TemplateParameters.ofPresent(
            "holderScope",
            change.getHolderScope(),
            "holderOfficeCode",
            officeName(change.getHolderOfficeCode(), currentOffices)));
  }

  private Object institutionName(UUID institutionId, Map<UUID, Institution> institutionsById) {
    Institution institution = institutionsById.get(institutionId);
    if (institution == null) {
      return null;
    }
    return keyedValue(institution.getName(), institution.getNameKey());
  }

  private Object jurisdictionName(UUID jurisdictionId, Map<UUID, String> jurisdictionsById) {
    return jurisdictionsById.get(jurisdictionId);
  }

  private Object officeName(String officeCode, Map<String, Office> officesByCode) {
    Office office = officesByCode.get(officeCode);
    if (office == null) {
      return null;
    }
    return keyedValue(office.getName(), office.getNameKey());
  }

  private Map<String, Object> keyedValue(String value, String key) {
    return TemplateParameters.ofPresent("value", value, "valueKey", key);
  }

  private ConstitutionChangeOperation changeOperation(ConstitutionInstitutionChangeAction action) {
    return ConstitutionChangeOperation.valueOf(action.name());
  }

  private ConstitutionChangeOperation changeOperation(ConstitutionOfficeChangeAction action) {
    return ConstitutionChangeOperation.valueOf(action.name());
  }

  private void disbandPolity(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    Polity polity =
        polities
            .findEntityById(motion.getPolityId())
            .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found."));
    if (polity.isDisbanded()) {
      throw ApiException.conflict(
          "polity_already_disbanded", "This polity has already been disbanded.");
    }
    officeTerms
        .findEntitiesByPolityIdAndStatus(motion.getPolityId(), OfficeTermStatus.ACTIVE)
        .forEach(term -> term.end(now));
    officeTerms.flush();
    memberships
        .findEntitiesByPolityIdAndStatusOrderByAdmittedAtAsc(
            motion.getPolityId(), MembershipStatus.ACTIVE)
        .forEach(
            member ->
                revocations.stage(
                    revocationPlanner.participation(
                        member.getAuthorizationSubject(), motion.getPolityId())));
    polity.disband(now);
    polities.saveAndFlush(polity);
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.POLITY_DISBANDED,
        polity.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.POLITY_DISBANDED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.POLITY_DISBANDED,
            TemplateParameters.with(
                motion.getTemplateParams(),
                "polityName",
                polity.getName(),
                "motionBody",
                motion.getBody(),
                "motionBodyKey",
                motion.getBodyKey())),
        now);
  }

  private Map<UUID, UUID> copyInstitutions(
      ConstitutionVersion current,
      ConstitutionVersion amended,
      List<ConstitutionInstitutionChangeProposalProjection> institutionChanges) {
    Map<UUID, ConstitutionInstitutionChangeProposalProjection> changesByInstitutionId =
        institutionChanges.stream()
            .filter(change -> change.getInstitutionId() != null)
            .collect(
                java.util.stream.Collectors.toMap(
                    ConstitutionInstitutionChangeProposalProjection::getInstitutionId,
                    change -> change));
    List<ConstitutionInstitutionChangeProposalProjection> createChanges =
        institutionChanges.stream()
            .filter(change -> change.getAction() == ConstitutionInstitutionChangeAction.CREATE)
            .toList();
    List<UUID> jurisdictionIds =
        jurisdictions.findEntitiesByPolityId(current.getPolityId()).stream()
            .map(jurisdiction -> jurisdiction.getId())
            .toList();
    Map<UUID, UUID> institutionIds = new HashMap<>();
    institutions
        .findEntitiesByConstitutionVersionId(current.getId())
        .forEach(
            institution -> {
              ConstitutionInstitutionChangeProposalProjection change =
                  changesByInstitutionId.remove(institution.getId());
              if (change == null) {
                Institution copied = institutions.saveAndFlush(institution.copyTo(amended.getId()));
                institutionIds.put(institution.getId(), copied.getId());
                return;
              }
              switch (change.getAction()) {
                case CREATE ->
                    throw ApiException.conflict(
                        "institution_already_exists",
                        "This institution already exists in the constitution.");
                case REVISE -> {
                  UUID jurisdictionId =
                      valueOr(change.getJurisdictionId(), institution.getJurisdictionId());
                  requireKnownJurisdiction(jurisdictionId, jurisdictionIds);
                  Institution copied =
                      institutions.saveAndFlush(
                          institution.copyWith(
                              amended.getId(),
                              jurisdictionId,
                              valueOr(change.getName(), institution.getName()),
                              change.getName() == null ? institution.getNameKey() : null,
                              valueOr(change.getKind(), institution.getKind())));
                  institutionIds.put(institution.getId(), copied.getId());
                }
                case RETIRE -> {
                  // No copied institution in the amended constitution.
                }
              }
            });
    if (!changesByInstitutionId.isEmpty()) {
      throw ApiException.notFound("institution_not_found", "Institution not found.");
    }
    createChanges.forEach(
        change -> {
          requireKnownJurisdiction(change.getJurisdictionId(), jurisdictionIds);
          institutions.saveAndFlush(
              new Institution(
                  amended.getPolityId(),
                  change.getJurisdictionId(),
                  amended.getId(),
                  change.getName(),
                  change.getKind()));
        });
    return institutionIds;
  }

  private void copyProcedures(
      ConstitutionVersion current,
      ConstitutionVersion amended,
      Map<UUID, UUID> institutionIds,
      Map<UUID, InstitutionKind> institutionKinds,
      Map<String, ConstitutionProcedureChangeProposalProjection> procedureChanges) {
    procedures
        .findEntitiesByConstitutionVersionId(current.getId())
        .forEach(
            procedure -> {
              ConstitutionProcedureChangeProposalProjection change =
                  procedureChanges.get(procedure.getCode());
              UUID institutionId =
                  change == null || change.getInstitutionId() == null
                      ? institutionIds.get(procedure.getInstitutionId())
                      : institutionIds.get(change.getInstitutionId());
              if (institutionId == null) {
                throw ApiException.badRequest(
                    "procedure_institution_missing",
                    "Procedures must refer to an institution in the amended constitution.");
              }
              UUID sourceInstitutionId =
                  change == null || change.getInstitutionId() == null
                      ? procedure.getInstitutionId()
                      : change.getInstitutionId();
              requireCompatibleProcedureInstitution(
                  procedure.getEffectType(), sourceInstitutionId, institutionKinds);
              if (change == null) {
                procedures.save(procedure.copyTo(amended.getId(), institutionId));
              } else {
                ProcedureElectorate electorate =
                    valueOr(change.getElectorate(), procedure.getElectorate());
                Procedure copied =
                    procedure.copyWithRules(
                        amended.getId(),
                        institutionId,
                        valueOr(change.getQuorumNumerator(), procedure.getQuorumNumerator()),
                        valueOr(change.getQuorumDenominator(), procedure.getQuorumDenominator()),
                        valueOr(change.getThreshold(), procedure.getThreshold()),
                        electorate,
                        resultingElectorateOfficeCode(electorate, change, procedure),
                        valueOr(
                            change.getMinimumElectorCount(), procedure.getMinimumElectorCount()),
                        valueOr(change.getMinimumNoticeHours(), procedure.getMinimumNoticeHours()),
                        valueOr(change.getVotingPeriodHours(), procedure.getVotingPeriodHours()));
                requireSufficientActiveMembers(copied);
                procedures.save(copied);
              }
            });
  }

  private int valueOr(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private <T> T valueOr(T value, T fallback) {
    return value == null ? fallback : value;
  }

  private Set<String> copyOffices(
      ConstitutionVersion current,
      ConstitutionVersion amended,
      List<ConstitutionOfficeChangeProposalProjection> officeChanges,
      OffsetDateTime now) {
    Map<String, ConstitutionOfficeChangeProposalProjection> changesByOfficeCode =
        officeChanges.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ConstitutionOfficeChangeProposalProjection::getOfficeCode, change -> change));
    Set<String> officeCodes = new HashSet<>();
    offices
        .findEntitiesByConstitutionVersionIdOrderByName(current.getId())
        .forEach(
            office -> {
              ConstitutionOfficeChangeProposalProjection change =
                  changesByOfficeCode.remove(office.getCode());
              if (change == null) {
                offices.save(office.copyTo(amended.getId()));
                officeCodes.add(office.getCode());
                return;
              }
              switch (change.getAction()) {
                case CREATE ->
                    throw ApiException.conflict(
                        "office_already_exists", "This office already exists in the constitution.");
                case REVISE -> {
                  int seatCount = valueOr(change.getSeatCount(), office.getSeatCount());
                  requireOfficeSeatCapacity(
                      current.getPolityId(), office.getCode(), seatCount, now);
                  offices.save(
                      office.copyWith(
                          amended.getId(),
                          valueOr(change.getJurisdictionId(), office.getJurisdictionId()),
                          valueOr(change.getName(), office.getName()),
                          valueOr(change.getDescription(), office.getDescription()),
                          change.getName() == null ? office.getNameKey() : null,
                          change.getDescription() == null ? office.getDescriptionKey() : null,
                          valueOr(change.getTermLengthDays(), office.getTermLengthDays()),
                          seatCount));
                  officeCodes.add(office.getCode());
                }
                case RETIRE -> retireOffice(current.getPolityId(), office.getCode(), now);
              }
            });
    changesByOfficeCode
        .values()
        .forEach(
            change -> {
              if (change.getAction() != ConstitutionOfficeChangeAction.CREATE) {
                throw ApiException.notFound("office_not_found", "Office not found.");
              }
              offices.save(
                  new Office(
                      amended.getPolityId(),
                      amended.getId(),
                      valueOr(change.getJurisdictionId(), currentJurisdictionId(current)),
                      change.getOfficeCode(),
                      change.getName(),
                      change.getDescription(),
                      change.getTermLengthDays(),
                      change.getSeatCount()));
              officeCodes.add(change.getOfficeCode());
            });
    return officeCodes;
  }

  private void copyPowers(
      ConstitutionVersion current,
      ConstitutionVersion amended,
      Map<PowerCode, ConstitutionPowerChangeProposalProjection> powerChanges,
      Set<String> officeCodes) {
    powers
        .findEntitiesByConstitutionVersionId(current.getId())
        .forEach(
            power -> {
              ConstitutionPowerChangeProposalProjection change =
                  powerChanges.remove(power.getCode());
              ConstitutionalPower copied =
                  change == null
                      ? power.copyTo(amended.getId())
                      : power.copyWithHolder(
                          amended.getId(), change.getHolderScope(), change.getHolderOfficeCode());
              requireAllowedPowerHolder(copied.getCode(), copied.getHolderScope());
              requireKnownPowerOffice(
                  copied.getHolderScope(), copied.getHolderOfficeCode(), officeCodes);
              powers.save(copied);
            });
    if (!powerChanges.isEmpty()) {
      throw ApiException.notFound("power_not_found", "Constitutional power not found.");
    }
  }

  private UUID currentJurisdictionId(ConstitutionVersion current) {
    return jurisdictions
        .findEntityByPolityIdAndKind(current.getPolityId(), JurisdictionKind.ROOT)
        .map(jurisdiction -> jurisdiction.getId())
        .orElseThrow(
            () -> ApiException.notFound("jurisdiction_not_found", "Jurisdiction not found."));
  }

  private Map<UUID, InstitutionKind> resultingInstitutionKinds(
      ConstitutionVersion current,
      List<ConstitutionInstitutionChangeProposalProjection> institutionChanges) {
    Map<UUID, InstitutionKind> kinds = new HashMap<>();
    institutions
        .findEntitiesByConstitutionVersionId(current.getId())
        .forEach(institution -> kinds.put(institution.getId(), institution.getKind()));
    institutionChanges.forEach(
        change -> {
          switch (change.getAction()) {
            case CREATE -> {
              // Newly created institutions cannot be procedure targets in the same amendment.
            }
            case REVISE ->
                kinds.put(
                    change.getInstitutionId(),
                    valueOr(change.getKind(), kinds.get(change.getInstitutionId())));
            case RETIRE -> kinds.remove(change.getInstitutionId());
          }
        });
    return kinds;
  }

  private void requireCompatibleProcedureInstitution(
      EffectType effectType, UUID institutionId, Map<UUID, InstitutionKind> institutionKinds) {
    InstitutionKind institutionKind = institutionKinds.get(institutionId);
    if (institutionKind != effectType.requiredInstitutionKind()) {
      throw ApiException.badRequest(
          "procedure_institution_kind_mismatch",
          "Procedures must belong to an institution kind compatible with their effect.");
    }
  }

  private String resultingElectorateOfficeCode(
      ProcedureElectorate electorate,
      ConstitutionProcedureChangeProposalProjection change,
      Procedure currentProcedure) {
    if (electorate != ProcedureElectorate.OFFICE_HOLDERS) {
      return null;
    }
    return valueOr(change.getElectorateOfficeCode(), currentProcedure.getElectorateOfficeCode());
  }

  private void requireSufficientActiveMembers(Procedure procedure) {
    if (procedure.getElectorate() == ProcedureElectorate.ACTIVE_MEMBERS
        && memberships.countByPolityIdAndStatus(procedure.getPolityId(), MembershipStatus.ACTIVE)
            < procedure.getMinimumElectorCount()) {
      throw ApiException.badRequest(
          "procedure_electorate_active_member_capacity_insufficient",
          "Active-member procedure electorates must not require more electors than the polity has.");
    }
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

  private void requireKnownJurisdiction(UUID jurisdictionId, List<UUID> jurisdictionIds) {
    if (!jurisdictionIds.contains(jurisdictionId)) {
      throw ApiException.badRequest(
          "constitution_change_jurisdiction_not_found",
          "Constitution changes must refer to an existing jurisdiction.");
    }
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

  private void retireOffice(UUID polityId, String officeCode, OffsetDateTime now) {
    officeTerms
        .findEntitiesByPolityIdAndOfficeCodeAndStatus(polityId, officeCode, OfficeTermStatus.ACTIVE)
        .forEach(term -> term.end(now));
    officeTerms.flush();
  }

  private List<OfficeElectionCandidateOption> electionCandidates(
      UUID motionId, OffsetDateTime now) {
    return officeElectionCandidates
        .findEntitiesByMotionIdAndStatus(motionId, OfficeElectionCandidateStatus.ACCEPTED)
        .stream()
        .map(
            candidate -> {
              Membership membership = membership(candidate.getMembershipId());
              if (!membershipService.hasPoliticalStanding(membership.getId(), now)) {
                return null;
              }
              return new OfficeElectionCandidateOption(
                  membership.getId(), membership.getDisplayName());
            })
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private Membership membership(UUID membershipId) {
    return memberships
        .findEntityById(membershipId)
        .orElseThrow(() -> ApiException.notFound("member_not_found", "Member not found."));
  }
}
