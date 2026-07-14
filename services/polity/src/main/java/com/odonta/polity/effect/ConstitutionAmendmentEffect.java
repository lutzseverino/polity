package com.odonta.polity.effect;

import com.odonta.common.api.ApiException;
import com.odonta.polity.model.ConstitutionChangeItem;
import com.odonta.polity.model.ConstitutionChangeKind;
import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.model.ConstitutionInstitutionChangeAction;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.ConstitutionTemplateKey;
import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalPower;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.JurisdictionKind;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Office;
import com.odonta.polity.model.OfficeTermStatus;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.TemplateParameters;
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
import com.odonta.polity.repository.InstitutionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ProcedureRepository;
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
final class ConstitutionAmendmentEffect implements MotionEffect {
  private final ConstitutionAmendmentProposalRepository amendmentProposals;
  private final ConstitutionInstitutionChangeProposalRepository institutionChangeProposals;
  private final ConstitutionOfficeChangeProposalRepository officeChangeProposals;
  private final ConstitutionPowerChangeProposalRepository powerChangeProposals;
  private final ConstitutionProcedureChangeProposalRepository procedureChangeProposals;
  private final ConstitutionVersionRepository constitutions;
  private final ConstitutionalPowerRepository powers;
  private final InstitutionRepository institutions;
  private final JurisdictionRepository jurisdictions;
  private final OfficeRepository offices;
  private final OfficeTermRepository officeTerms;
  private final OfficialRecordService officialRecords;
  private final ProcedureRepository procedures;

  @Override
  public void apply(
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

  @Override
  public EffectType type() {
    return EffectType.AMEND_CONSTITUTION;
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
        proposedOrCurrent(
            change.getName(), institution == null ? "institution" : institution.getName()),
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
            "officeElectionMethod",
            change.getOfficeElectionMethod(),
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
        proposedOrCurrent(
            change.getName(), office == null ? change.getOfficeCode() : office.getName()),
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
                      proposedOrCurrent(
                          change.getJurisdictionId(), institution.getJurisdictionId());
                  requireKnownJurisdiction(jurisdictionId, jurisdictionIds);
                  Institution copied =
                      institutions.saveAndFlush(
                          institution.copyWith(
                              amended.getId(),
                              jurisdictionId,
                              proposedOrCurrent(change.getName(), institution.getName()),
                              change.getName() == null ? institution.getNameKey() : null,
                              proposedOrCurrent(change.getKind(), institution.getKind())));
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
                    proposedOrCurrent(change.getElectorate(), procedure.getElectorate());
                Procedure copied =
                    procedure.copyWithRules(
                        amended.getId(),
                        institutionId,
                        proposedOrCurrent(
                            change.getQuorumNumerator(), procedure.getQuorumNumerator()),
                        proposedOrCurrent(
                            change.getQuorumDenominator(), procedure.getQuorumDenominator()),
                        proposedOrCurrent(change.getThreshold(), procedure.getThreshold()),
                        electorate,
                        resultingElectorateOfficeCode(electorate, change, procedure),
                        proposedOrCurrent(
                            change.getMinimumElectorCount(), procedure.getMinimumElectorCount()),
                        proposedOrCurrent(
                            change.getMinimumNoticeHours(), procedure.getMinimumNoticeHours()),
                        proposedOrCurrent(
                            change.getVotingPeriodHours(), procedure.getVotingPeriodHours()),
                        proposedOrCurrent(
                            change.getOfficeElectionMethod(), procedure.getOfficeElectionMethod()));
                procedures.save(copied);
              }
            });
  }

  private int proposedOrCurrent(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private <T> T proposedOrCurrent(T value, T fallback) {
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
                  int seatCount = proposedOrCurrent(change.getSeatCount(), office.getSeatCount());
                  requireOfficeSeatCapacity(
                      current.getPolityId(), office.getCode(), seatCount, now);
                  offices.save(
                      office.copyWith(
                          amended.getId(),
                          proposedOrCurrent(change.getJurisdictionId(), office.getJurisdictionId()),
                          proposedOrCurrent(change.getName(), office.getName()),
                          proposedOrCurrent(change.getDescription(), office.getDescription()),
                          change.getName() == null ? office.getNameKey() : null,
                          change.getDescription() == null ? office.getDescriptionKey() : null,
                          proposedOrCurrent(change.getTermLengthDays(), office.getTermLengthDays()),
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
                      proposedOrCurrent(change.getJurisdictionId(), currentJurisdictionId(current)),
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
                    proposedOrCurrent(change.getKind(), kinds.get(change.getInstitutionId())));
            case RETIRE -> kinds.remove(change.getInstitutionId());
          }
        });
    return kinds;
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

  private String resultingElectorateOfficeCode(
      ProcedureElectorate electorate,
      ConstitutionProcedureChangeProposalProjection change,
      Procedure currentProcedure) {
    if (electorate != ProcedureElectorate.OFFICE_HOLDERS) {
      return null;
    }
    return proposedOrCurrent(
        change.getElectorateOfficeCode(), currentProcedure.getElectorateOfficeCode());
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

  private void retireOffice(UUID polityId, String officeCode, OffsetDateTime now) {
    officeTerms
        .findEntitiesByPolityIdAndOfficeCodeAndStatus(polityId, officeCode, OfficeTermStatus.ACTIVE)
        .forEach(term -> term.end(now));
    officeTerms.flush();
  }
}
