package com.odonta.polity.service;

import com.odonta.common.api.ApiException;
import com.odonta.polity.evaluator.OfficeElectionEvaluator;
import com.odonta.polity.model.Appeal;
import com.odonta.polity.model.AppealProposal;
import com.odonta.polity.model.ConstitutionAmendmentProposal;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.ConstitutionOfficeChangeProposal;
import com.odonta.polity.model.ConstitutionPowerChangeProposal;
import com.odonta.polity.model.ConstitutionProcedureChangeProposal;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
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
import com.odonta.polity.model.Polity;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.Resolution;
import com.odonta.polity.model.Sanction;
import com.odonta.polity.model.SanctionProposal;
import com.odonta.polity.repository.AppealProposalRepository;
import com.odonta.polity.repository.AppealRepository;
import com.odonta.polity.repository.ConstitutionAmendmentProposalRepository;
import com.odonta.polity.repository.ConstitutionOfficeChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionPowerChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionProcedureChangeProposalRepository;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.ConstitutionalPowerRepository;
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.OfficeElectionBallotRepository;
import com.odonta.polity.repository.OfficeElectionCandidateRepository;
import com.odonta.polity.repository.OfficeElectionProposalRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.repository.ProcedureRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.SanctionProposalRepository;
import com.odonta.polity.repository.SanctionRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EffectApplicationService {
  private final AppealProposalRepository appealProposals;
  private final AppealRepository appeals;
  private final ConstitutionAmendmentProposalRepository amendmentProposals;
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals;
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals;
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final InstitutionRepository institutions;
  private final MotionElectorRepository electors;
  private final OfficeElectionBallotRepository officeElectionBallots;
  private final OfficeElectionCandidateRepository officeElectionCandidates;
  private final OfficeElectionEvaluator officeElections;
  private final OfficeElectionProposalRepository officeElectionProposals;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final PolityRepository polities;
  private final OfficialRecordService officialRecords;
  private final ProcedureRepository procedures;
  private final ResolutionRepository resolutions;
  private final SanctionProposalRepository sanctionProposals;
  private final SanctionRepository sanctions;
  private final MembershipService membershipService;

  void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    switch (motion.getEffectType()) {
      case ADOPT_RESOLUTION -> applyResolution(motion, actor, constitution, now);
      case ASSIGN_OFFICE ->
          throw ApiException.conflict(
              "legacy_office_assignment",
              "Direct office assignment is a legacy effect and can no longer be applied.");
      case ELECT_OFFICE -> electOffice(motion, actor, constitution, now);
      case APPLY_SANCTION -> applySanction(motion, actor, constitution, now);
      case GRANT_APPEAL -> grantAppeal(motion, actor, constitution, now);
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
            Map.of("motionTitle", motion.getTitle(), "motionBody", motion.getBody())),
        now);
  }

  private void electOffice(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    var proposal =
        officeElectionProposals
            .findByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "office_election_proposal_not_found",
                        "Office election proposal not found."));
    Office office =
        offices
            .findByIdAndPolityId(proposal.getOfficeId(), motion.getPolityId())
            .orElseThrow(() -> ApiException.notFound("office_not_found", "Office not found."));
    Procedure procedure =
        procedures
            .findById(motion.getProcedureId())
            .orElseThrow(
                () -> ApiException.notFound("procedure_not_found", "Procedure not found."));
    OfficeElectionTallyResult result =
        officeElections.evaluate(
            procedure,
            Math.toIntExact(electors.countByMotionId(motion.getId())),
            electionCandidates(motion.getId(), now),
            officeElectionBallots.findByMotionId(motion.getId()));
    if (!result.passed() || result.winnerMembershipId() == null) {
      throw ApiException.conflict(
          "office_election_not_passed", "Only passed office elections can assign office terms.");
    }
    officeTerms
        .findByPolityIdAndOfficeCodeAndStatus(
            motion.getPolityId(), office.getCode(), OfficeTermStatus.ACTIVE)
        .forEach(term -> term.end(now));
    officeTerms.flush();
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
            Map.of(
                "memberName",
                result.winnerName(),
                "officeName",
                office.getName(),
                "termLengthDays",
                office.getTermLengthDays())),
        now);
  }

  private void applySanction(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    SanctionProposal proposal =
        sanctionProposals
            .findByMotionId(motion.getId())
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
    Membership target = membershipService.get(proposal.getTargetMembershipId());
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
            Map.of(
                "memberName",
                target.getDisplayName(),
                "sanctionType",
                proposal.getType().name(),
                "durationDays",
                proposal.getDurationDays(),
                "reason",
                proposal.getReason())),
        now);
  }

  private void grantAppeal(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    AppealProposal proposal =
        appealProposals
            .findByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "appeal_proposal_not_found", "Appeal proposal not found."));
    Sanction sanction =
        sanctions
            .findByIdAndPolityId(proposal.getSanctionId(), motion.getPolityId())
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
    Membership appellant = membershipService.get(proposal.getAppellantMembershipId());
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
            Map.of("memberName", appellant.getDisplayName(), "reason", proposal.getReason())),
        now);
  }

  private void amendConstitution(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    ConstitutionAmendmentProposal proposal =
        amendmentProposals
            .findByMotionId(motion.getId())
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "amendment_proposal_not_found", "Amendment proposal not found."));
    ConstitutionVersion current =
        constitutions
            .findById(constitution.getId())
            .orElseThrow(
                () -> ApiException.notFound("constitution_not_found", "Constitution not found."));
    Map<String, ConstitutionProcedureChangeProposal> procedureChanges =
        procedureChangeProposals.findByAmendmentProposalId(proposal.getId()).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ConstitutionProcedureChangeProposal::getProcedureCode, change -> change));
    List<ConstitutionOfficeChangeProposal> officeChanges =
        officeChangeProposals.findByAmendmentProposalId(proposal.getId());
    Map<PowerCode, ConstitutionPowerChangeProposal> powerChanges =
        powerChangeProposals.findByAmendmentProposalId(proposal.getId()).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ConstitutionPowerChangeProposal::getPowerCode, change -> change));
    if (procedureChanges.isEmpty() && officeChanges.isEmpty() && powerChanges.isEmpty()) {
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
                ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackTitle(current.getVersion() + 1),
                ConstitutionTemplateKey.STRUCTURED_CHARTER.fallbackBody(),
                ConstitutionTemplateKey.STRUCTURED_CHARTER,
                now));
    copyInstitutions(current.getId(), amended.getId(), procedureChanges);
    Set<String> officeCodes = copyOffices(current, amended, officeChanges, now);
    copyPowers(current, amended, powerChanges, officeCodes);
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
            Map.of(
                "constitutionVersion",
                amended.getVersion(),
                "amendmentBody",
                proposal.getBody(),
                "changeSummary",
                proposal.getChangeSummary())),
        now);
  }

  private void disbandPolity(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    Polity polity =
        polities
            .findById(motion.getPolityId())
            .orElseThrow(() -> ApiException.notFound("polity_not_found", "Polity not found."));
    if (polity.isDisbanded()) {
      throw ApiException.conflict("polity_disbanded", "This polity has already been disbanded.");
    }
    officeTerms
        .findByPolityIdAndStatus(motion.getPolityId(), OfficeTermStatus.ACTIVE)
        .forEach(term -> term.end(now));
    officeTerms.flush();
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
            Map.of("polityName", polity.getName(), "motionBody", motion.getBody())),
        now);
  }

  private void copyInstitutions(
      UUID currentConstitutionId,
      UUID amendedConstitutionId,
      Map<String, ConstitutionProcedureChangeProposal> procedureChanges) {
    Map<UUID, UUID> institutionIds = new HashMap<>();
    institutions
        .findByConstitutionVersionId(currentConstitutionId)
        .forEach(
            institution -> {
              Institution copied =
                  institutions.saveAndFlush(institution.copyTo(amendedConstitutionId));
              institutionIds.put(institution.getId(), copied.getId());
            });
    procedures
        .findByConstitutionVersionId(currentConstitutionId)
        .forEach(
            procedure -> {
              UUID institutionId = institutionIds.get(procedure.getInstitutionId());
              if (institutionId != null) {
                ConstitutionProcedureChangeProposal change =
                    procedureChanges.get(procedure.getCode());
                if (change == null) {
                  procedures.save(procedure.copyTo(amendedConstitutionId, institutionId));
                } else {
                  procedures.save(
                      procedure.copyWithRules(
                          amendedConstitutionId,
                          institutionId,
                          valueOr(change.getQuorumNumerator(), procedure.getQuorumNumerator()),
                          valueOr(change.getQuorumDenominator(), procedure.getQuorumDenominator()),
                          valueOr(change.getThreshold(), procedure.getThreshold()),
                          valueOr(change.getElectorate(), procedure.getElectorate()),
                          valueOr(
                              change.getElectorateOfficeCode(),
                              procedure.getElectorateOfficeCode()),
                          valueOr(
                              change.getMinimumNoticeHours(), procedure.getMinimumNoticeHours()),
                          valueOr(
                              change.getVotingPeriodHours(), procedure.getVotingPeriodHours())));
                }
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
      List<ConstitutionOfficeChangeProposal> officeChanges,
      OffsetDateTime now) {
    Map<String, ConstitutionOfficeChangeProposal> changesByOfficeCode =
        officeChanges.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ConstitutionOfficeChangeProposal::getOfficeCode, change -> change));
    Set<String> officeCodes = new HashSet<>();
    offices
        .findByConstitutionVersionIdOrderByName(current.getId())
        .forEach(
            office -> {
              ConstitutionOfficeChangeProposal change =
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
                  offices.save(
                      office.copyWith(
                          amended.getId(),
                          valueOr(change.getName(), office.getName()),
                          valueOr(change.getDescription(), office.getDescription()),
                          change.getName() == null ? office.getNameKey() : null,
                          change.getDescription() == null ? office.getDescriptionKey() : null,
                          valueOr(change.getTermLengthDays(), office.getTermLengthDays())));
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
                      currentJurisdictionId(current),
                      change.getOfficeCode(),
                      change.getName(),
                      change.getDescription(),
                      change.getTermLengthDays()));
              officeCodes.add(change.getOfficeCode());
            });
    return officeCodes;
  }

  private void copyPowers(
      ConstitutionVersion current,
      ConstitutionVersion amended,
      Map<PowerCode, ConstitutionPowerChangeProposal> powerChanges,
      Set<String> officeCodes) {
    powers
        .findByConstitutionVersionId(current.getId())
        .forEach(
            power -> {
              ConstitutionPowerChangeProposal change = powerChanges.remove(power.getCode());
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
    return institutions.findByConstitutionVersionId(current.getId()).stream()
        .findFirst()
        .map(Institution::getJurisdictionId)
        .orElseThrow(
            () -> ApiException.notFound("institution_not_found", "Institution not found."));
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
        .findByPolityIdAndOfficeCodeAndStatus(polityId, officeCode, OfficeTermStatus.ACTIVE)
        .forEach(term -> term.end(now));
    officeTerms.flush();
  }

  private List<OfficeElectionCandidateOption> electionCandidates(
      UUID motionId, OffsetDateTime now) {
    return officeElectionCandidates
        .findByMotionIdAndStatus(motionId, OfficeElectionCandidateStatus.ACCEPTED)
        .stream()
        .map(
            candidate -> {
              Membership membership = membershipService.get(candidate.getMembershipId());
              if (!membershipService.hasPoliticalStanding(membership, now)) {
                return null;
              }
              return new OfficeElectionCandidateOption(
                  membership.getId(), membership.getDisplayName());
            })
        .filter(java.util.Objects::nonNull)
        .toList();
  }
}
