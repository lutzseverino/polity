package com.odonta.polity.evaluator;

import com.odonta.polity.input.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.input.CreateInstitutionChangeInput;
import com.odonta.polity.input.CreateOfficeChangeInput;
import com.odonta.polity.input.CreatePowerChangeInput;
import com.odonta.polity.input.CreateProcedureChangeInput;
import com.odonta.polity.input.ValidatedConstitutionAmendmentInput;
import com.odonta.polity.model.ConstitutionAmendmentPowerState;
import com.odonta.polity.model.ConstitutionAmendmentProcedureState;
import com.odonta.polity.model.ConstitutionAmendmentState;
import com.odonta.polity.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConstitutionAmendmentEvaluator {
  public ValidatedConstitutionAmendmentInput evaluate(
      ConstitutionAmendmentState state, CreateConstitutionAmendmentMotionInput input) {
    List<CreateInstitutionChangeInput> institutionChanges =
        evaluateInstitutionChanges(state, input.institutionChanges());
    List<CreateOfficeChangeInput> officeChanges =
        evaluateOfficeChanges(state, input.officeChanges());
    List<CreateProcedureChangeInput> procedureChanges =
        evaluateProcedureChanges(
            state, input.procedureChanges(), institutionChanges, officeChanges);
    List<CreatePowerChangeInput> powerChanges =
        evaluatePowerChanges(state, input.powerChanges(), officeChanges);
    return new ValidatedConstitutionAmendmentInput(
        institutionChanges, procedureChanges, officeChanges, powerChanges);
  }

  private List<CreateInstitutionChangeInput> evaluateInstitutionChanges(
      ConstitutionAmendmentState state, List<CreateInstitutionChangeInput> proposed) {
    List<CreateInstitutionChangeInput> changes =
        proposedChanges(proposed).stream().map(this::normalized).toList();
    for (CreateInstitutionChangeInput change : changes) {
      boolean institutionExists =
          change.institutionId() != null
              && state.institutionKinds().containsKey(change.institutionId());
      switch (change.action()) {
        case CREATE -> requireKnownJurisdiction(change.jurisdictionId(), state.jurisdictionIds());
        case REVISE -> {
          requireInstitution(institutionExists);
          if (change.jurisdictionId() != null) {
            requireKnownJurisdiction(change.jurisdictionId(), state.jurisdictionIds());
          }
        }
        case RETIRE -> requireInstitution(institutionExists);
      }
    }
    return changes;
  }

  private List<CreateOfficeChangeInput> evaluateOfficeChanges(
      ConstitutionAmendmentState state, List<CreateOfficeChangeInput> proposed) {
    List<CreateOfficeChangeInput> changes =
        proposedChanges(proposed).stream().map(this::normalized).toList();
    for (CreateOfficeChangeInput change : changes) {
      boolean officeExists = state.officeCodes().contains(change.code());
      switch (change.action()) {
        case CREATE -> {
          if (officeExists) {
            conflict("office_already_exists", "This office already exists in the constitution.");
          }
          if (change.jurisdictionId() != null) {
            requireKnownJurisdiction(change.jurisdictionId(), state.jurisdictionIds());
          }
        }
        case REVISE -> {
          requireOffice(officeExists);
          if (change.jurisdictionId() != null) {
            requireKnownJurisdiction(change.jurisdictionId(), state.jurisdictionIds());
          }
          if (change.seatCount() != null
              && state.activeOfficeTermCounts().getOrDefault(change.code(), 0L)
                  > change.seatCount()) {
            invalid(
                "office_seat_capacity_insufficient",
                "Office seat count cannot be reduced below the current active terms.");
          }
        }
        case RETIRE -> requireOffice(officeExists);
      }
    }
    return changes;
  }

  private List<CreateProcedureChangeInput> evaluateProcedureChanges(
      ConstitutionAmendmentState state,
      List<CreateProcedureChangeInput> proposed,
      List<CreateInstitutionChangeInput> institutionChanges,
      List<CreateOfficeChangeInput> officeChanges) {
    Map<UUID, InstitutionKind> institutionKinds =
        resultingInstitutionKinds(state, institutionChanges);
    Set<String> officeCodes = resultingOfficeCodes(state, officeChanges);
    List<CreateProcedureChangeInput> changes = new ArrayList<>();
    for (CreateProcedureChangeInput proposedChange : proposedChanges(proposed)) {
      ConstitutionAmendmentProcedureState procedure =
          state.procedures().get(proposedChange.procedureCode());
      if (procedure == null) {
        missing("procedure_not_found", "Procedure not found.");
      }
      CreateProcedureChangeInput change = normalized(proposedChange);
      requireCompatibleProcedureThreshold(
          procedure.effectType(), proposedOrCurrent(change.threshold(), procedure.threshold()));
      requireCompatibleOfficeElectionMethod(
          procedure.effectType(),
          proposedOrCurrent(change.officeElectionMethod(), procedure.officeElectionMethod()));
      UUID institutionId = proposedOrCurrent(change.institutionId(), procedure.institutionId());
      requireKnownProcedureInstitution(institutionId, institutionKinds.keySet());
      requireCompatibleProcedureInstitution(
          procedure.effectType(), institutionId, institutionKinds);
      ProcedureElectorate electorate =
          proposedOrCurrent(change.electorate(), procedure.electorate());
      String electorateOfficeCode =
          resultingElectorateOfficeCode(electorate, change.electorateOfficeCode(), procedure);
      requireKnownElectorateOffice(electorate, electorateOfficeCode, officeCodes);
      changes.add(change);
    }
    Map<String, CreateProcedureChangeInput> changesByCode = procedureChangesByCode(changes);
    for (Map.Entry<String, ConstitutionAmendmentProcedureState> entry :
        state.procedures().entrySet()) {
      ConstitutionAmendmentProcedureState procedure = entry.getValue();
      CreateProcedureChangeInput change = changesByCode.get(entry.getKey());
      UUID institutionId =
          change == null
              ? procedure.institutionId()
              : proposedOrCurrent(change.institutionId(), procedure.institutionId());
      requireKnownProcedureInstitution(institutionId, institutionKinds.keySet());
      requireCompatibleProcedureInstitution(
          procedure.effectType(), institutionId, institutionKinds);
      ProcedureElectorate electorate =
          change == null
              ? procedure.electorate()
              : proposedOrCurrent(change.electorate(), procedure.electorate());
      String officeCode =
          change == null
              ? procedure.electorateOfficeCode()
              : resultingElectorateOfficeCode(electorate, change.electorateOfficeCode(), procedure);
      requireKnownElectorateOffice(electorate, officeCode, officeCodes);
    }
    return List.copyOf(changes);
  }

  private List<CreatePowerChangeInput> evaluatePowerChanges(
      ConstitutionAmendmentState state,
      List<CreatePowerChangeInput> proposed,
      List<CreateOfficeChangeInput> officeChanges) {
    Set<String> officeCodes = resultingOfficeCodes(state, officeChanges);
    List<CreatePowerChangeInput> changes =
        proposedChanges(proposed).stream().map(this::normalized).toList();
    for (CreatePowerChangeInput change : changes) {
      if (!state.powers().containsKey(change.powerCode())) {
        missing("power_not_found", "Constitutional power not found.");
      }
      requireAllowedPowerHolder(change.powerCode(), change.holderScope());
      requireKnownPowerOffice(change.holderScope(), change.holderOfficeCode(), officeCodes);
    }
    Map<PowerCode, CreatePowerChangeInput> changesByCode = powerChangesByCode(changes);
    for (Map.Entry<PowerCode, ConstitutionAmendmentPowerState> entry : state.powers().entrySet()) {
      ConstitutionAmendmentPowerState power = entry.getValue();
      CreatePowerChangeInput change = changesByCode.get(entry.getKey());
      PowerHolderScope holderScope = change == null ? power.holderScope() : change.holderScope();
      String holderOfficeCode =
          change == null ? power.holderOfficeCode() : change.holderOfficeCode();
      requireAllowedPowerHolder(entry.getKey(), holderScope);
      requireKnownPowerOffice(holderScope, normalizedOfficeCode(holderOfficeCode), officeCodes);
    }
    return changes;
  }

  private Map<UUID, InstitutionKind> resultingInstitutionKinds(
      ConstitutionAmendmentState state, List<CreateInstitutionChangeInput> institutionChanges) {
    Map<UUID, InstitutionKind> kinds = new HashMap<>(state.institutionKinds());
    for (CreateInstitutionChangeInput change : institutionChanges) {
      switch (change.action()) {
        case CREATE -> {
          // New institutions have no durable identifier and cannot be procedure targets yet.
        }
        case REVISE ->
            kinds.put(
                change.institutionId(),
                proposedOrCurrent(change.kind(), kinds.get(change.institutionId())));
        case RETIRE -> kinds.remove(change.institutionId());
      }
    }
    return kinds;
  }

  private Set<String> resultingOfficeCodes(
      ConstitutionAmendmentState state, List<CreateOfficeChangeInput> officeChanges) {
    Set<String> codes = new HashSet<>(state.officeCodes());
    for (CreateOfficeChangeInput change : officeChanges) {
      if (change.action() == ConstitutionOfficeChangeAction.RETIRE) {
        codes.remove(change.code());
      } else {
        codes.add(change.code());
      }
    }
    return codes;
  }

  private void requireInstitution(boolean exists) {
    if (!exists) {
      missing("institution_not_found", "Institution not found.");
    }
  }

  private void requireOffice(boolean exists) {
    if (!exists) {
      missing("office_not_found", "Office not found.");
    }
  }

  private void requireKnownJurisdiction(UUID jurisdictionId, Set<UUID> jurisdictionIds) {
    if (!jurisdictionIds.contains(jurisdictionId)) {
      invalid(
          "constitution_change_jurisdiction_not_found",
          "Constitution changes must refer to an existing jurisdiction.");
    }
  }

  private void requireKnownProcedureInstitution(UUID institutionId, Set<UUID> institutionIds) {
    if (!institutionIds.contains(institutionId)) {
      invalid(
          "procedure_institution_missing",
          "Procedures must refer to an institution in the amended constitution.");
    }
  }

  private void requireCompatibleProcedureInstitution(
      EffectType effectType, UUID institutionId, Map<UUID, InstitutionKind> institutionKinds) {
    if (!effectType.supportsInstitutionKind(institutionKinds.get(institutionId))) {
      invalid(
          "procedure_institution_kind_mismatch",
          "Procedures must belong to an institution kind compatible with their effect.");
    }
  }

  private void requireCompatibleProcedureThreshold(
      EffectType effectType, VotingThreshold threshold) {
    if (effectType == EffectType.ELECT_OFFICE
        && threshold != VotingThreshold.OFFICE_ELECTION_RESULT) {
      invalid(
          "office_election_result_threshold_required",
          "Office election procedures must use office-election result thresholds.");
    }
    if (effectType != EffectType.ELECT_OFFICE
        && threshold == VotingThreshold.OFFICE_ELECTION_RESULT) {
      invalid(
          "office_election_result_threshold_requires_election",
          "Office-election result thresholds can only be used by office election procedures.");
    }
  }

  private void requireCompatibleOfficeElectionMethod(
      EffectType effectType, OfficeElectionMethod officeElectionMethod) {
    if (effectType == EffectType.ELECT_OFFICE && officeElectionMethod == null) {
      invalid(
          "office_election_method_required",
          "Office election procedures must define an office election method.");
    }
    if (effectType != EffectType.ELECT_OFFICE && officeElectionMethod != null) {
      invalid(
          "office_election_method_requires_election",
          "Office election methods can only be used by office election procedures.");
    }
  }

  private void requireAllowedPowerHolder(PowerCode powerCode, PowerHolderScope holderScope) {
    if (powerCode.requiresActiveMemberHolder() && holderScope != PowerHolderScope.ACTIVE_MEMBER) {
      invalid("citizen_power_required", "Disbandment must remain an active citizen power.");
    }
  }

  private void requireKnownPowerOffice(
      PowerHolderScope holderScope, String holderOfficeCode, Set<String> officeCodes) {
    if (holderScope == PowerHolderScope.OFFICE && !officeCodes.contains(holderOfficeCode)) {
      invalid(
          "power_holder_office_missing",
          "Office-held powers must refer to an office in the amended constitution.");
    }
  }

  private void requireKnownElectorateOffice(
      ProcedureElectorate electorate, String electorateOfficeCode, Set<String> officeCodes) {
    if (electorate == ProcedureElectorate.OFFICE_HOLDERS
        && !officeCodes.contains(electorateOfficeCode)) {
      invalid(
          "procedure_electorate_office_missing",
          "Office-held procedure electorates must refer to an office in the amended constitution.");
    }
  }

  private String resultingElectorateOfficeCode(
      ProcedureElectorate electorate,
      String proposedOfficeCode,
      ConstitutionAmendmentProcedureState currentProcedure) {
    if (electorate != ProcedureElectorate.OFFICE_HOLDERS) {
      return null;
    }
    return proposedOrCurrent(
        normalizedProposalOfficeCode(proposedOfficeCode), currentProcedure.electorateOfficeCode());
  }

  private Map<PowerCode, CreatePowerChangeInput> powerChangesByCode(
      List<CreatePowerChangeInput> changes) {
    Map<PowerCode, CreatePowerChangeInput> byCode = new EnumMap<>(PowerCode.class);
    changes.forEach(change -> byCode.put(change.powerCode(), change));
    return byCode;
  }

  private Map<String, CreateProcedureChangeInput> procedureChangesByCode(
      List<CreateProcedureChangeInput> changes) {
    Map<String, CreateProcedureChangeInput> byCode = new HashMap<>();
    changes.forEach(change -> byCode.put(change.procedureCode(), change));
    return byCode;
  }

  private CreateInstitutionChangeInput normalized(CreateInstitutionChangeInput change) {
    return new CreateInstitutionChangeInput(
        change.action(),
        change.institutionId(),
        change.jurisdictionId(),
        trimmedProposalValue(change.name()),
        change.kind());
  }

  private CreateOfficeChangeInput normalized(CreateOfficeChangeInput change) {
    return new CreateOfficeChangeInput(
        change.action(),
        normalizedOfficeCode(change.code()),
        change.jurisdictionId(),
        trimmedProposalValue(change.name()),
        trimmedProposalValue(change.description()),
        change.termLengthDays(),
        change.seatCount());
  }

  private CreatePowerChangeInput normalized(CreatePowerChangeInput change) {
    return new CreatePowerChangeInput(
        change.powerCode(),
        change.holderScope(),
        normalizedProposalOfficeCode(change.holderOfficeCode()));
  }

  private CreateProcedureChangeInput normalized(CreateProcedureChangeInput change) {
    return new CreateProcedureChangeInput(
        change.procedureCode(),
        change.institutionId(),
        change.quorumNumerator(),
        change.quorumDenominator(),
        change.threshold(),
        change.officeElectionMethod(),
        change.electorate(),
        normalizedProposalOfficeCode(change.electorateOfficeCode()),
        change.minimumElectorCount(),
        change.minimumNoticeHours(),
        change.votingPeriodHours());
  }

  private <T> List<T> proposedChanges(List<T> values) {
    return values == null ? List.of() : values;
  }

  private <T> T proposedOrCurrent(T value, T fallback) {
    return value == null ? fallback : value;
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

  private void invalid(String code, String message) {
    throw new ConstitutionAmendmentEvaluationException(
        ConstitutionAmendmentViolationKind.INVALID, code, message);
  }

  private void missing(String code, String message) {
    throw new ConstitutionAmendmentEvaluationException(
        ConstitutionAmendmentViolationKind.MISSING_REFERENCE, code, message);
  }

  private void conflict(String code, String message) {
    throw new ConstitutionAmendmentEvaluationException(
        ConstitutionAmendmentViolationKind.CONFLICTING_STATE, code, message);
  }
}
