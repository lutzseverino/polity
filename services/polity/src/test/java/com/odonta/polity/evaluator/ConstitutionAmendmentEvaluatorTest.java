package com.odonta.polity.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.odonta.polity.input.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.input.CreateInstitutionChangeInput;
import com.odonta.polity.input.CreateOfficeChangeInput;
import com.odonta.polity.input.CreatePowerChangeInput;
import com.odonta.polity.input.CreateProcedureChangeInput;
import com.odonta.polity.model.ConstitutionAmendmentPowerState;
import com.odonta.polity.model.ConstitutionAmendmentProcedureState;
import com.odonta.polity.model.ConstitutionAmendmentState;
import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConstitutionAmendmentEvaluatorTest {
  private final ConstitutionAmendmentEvaluator evaluator = new ConstitutionAmendmentEvaluator();

  @Test
  void evaluatesAndNormalizesACompatiblePlan() {
    UUID jurisdictionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    ConstitutionAmendmentState state =
        state(jurisdictionId, institutionId, ProcedureElectorate.ACTIVE_MEMBERS, null, Map.of());
    CreateConstitutionAmendmentMotionInput input =
        new CreateConstitutionAmendmentMotionInput(
            "Rebalance",
            "Adjust the steward office.",
            List.of(
                new CreateInstitutionChangeInput(
                    ConstitutionChangeOperation.REVISE, institutionId, null, "  Council  ", null)),
            List.of(),
            List.of(
                new CreateOfficeChangeInput(
                    ConstitutionChangeOperation.REVISE,
                    "steward",
                    null,
                    "  Steward  ",
                    null,
                    null,
                    2)),
            List.of(
                new CreatePowerChangeInput(
                    PowerCode.REQUEST_CERTIFICATION, PowerHolderScope.OFFICE, "steward")));

    var plan = evaluator.evaluate(state, input);

    assertThat(plan.institutionChanges()).singleElement().extracting("name").isEqualTo("Council");
    assertThat(plan.officeChanges()).singleElement().extracting("name").isEqualTo("Steward");
    assertThat(plan.powerChanges())
        .singleElement()
        .extracting("holderOfficeCode")
        .isEqualTo("steward");
  }

  @Test
  void rejectsRetiringAnOfficeStillReferencedByAnUnchangedProcedure() {
    UUID jurisdictionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    ConstitutionAmendmentState state =
        state(
            jurisdictionId, institutionId, ProcedureElectorate.OFFICE_HOLDERS, "steward", Map.of());
    CreateConstitutionAmendmentMotionInput input =
        new CreateConstitutionAmendmentMotionInput(
            "Retire steward",
            "Remove the office.",
            List.of(),
            List.of(),
            List.of(
                new CreateOfficeChangeInput(
                    ConstitutionChangeOperation.RETIRE, "steward", null, null, null, null, null)),
            List.of());

    assertThatThrownBy(() -> evaluator.evaluate(state, input))
        .isInstanceOfSatisfying(
            ConstitutionAmendmentEvaluationException.class,
            exception -> {
              assertThat(exception.kind()).isEqualTo(ConstitutionAmendmentViolationKind.INVALID);
              assertThat(exception.code()).isEqualTo("procedure_electorate_office_missing");
            });
  }

  @Test
  void rejectsReducingSeatsBelowCurrentActiveTerms() {
    UUID jurisdictionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    ConstitutionAmendmentState state =
        state(
            jurisdictionId,
            institutionId,
            ProcedureElectorate.ACTIVE_MEMBERS,
            null,
            Map.of("steward", 2L));
    CreateConstitutionAmendmentMotionInput input =
        new CreateConstitutionAmendmentMotionInput(
            "Reduce seats",
            "Reduce the office.",
            List.of(),
            List.of(),
            List.of(
                new CreateOfficeChangeInput(
                    ConstitutionChangeOperation.REVISE, "steward", null, null, null, null, 1)),
            List.of());

    assertThatThrownBy(() -> evaluator.evaluate(state, input))
        .isInstanceOfSatisfying(
            ConstitutionAmendmentEvaluationException.class,
            exception -> {
              assertThat(exception.kind()).isEqualTo(ConstitutionAmendmentViolationKind.INVALID);
              assertThat(exception.code()).isEqualTo("office_seat_capacity_insufficient");
            });
  }

  @Test
  void reportsUnknownPowerAsAMissingReference() {
    UUID jurisdictionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    ConstitutionAmendmentState state =
        state(jurisdictionId, institutionId, ProcedureElectorate.ACTIVE_MEMBERS, null, Map.of());
    CreateConstitutionAmendmentMotionInput input =
        new CreateConstitutionAmendmentMotionInput(
            "Move appeal authority",
            "Change a missing power.",
            List.of(),
            List.of(),
            List.of(),
            List.of(
                new CreatePowerChangeInput(
                    PowerCode.INTRODUCE_APPEAL, PowerHolderScope.ACTIVE_MEMBER, null)));

    assertThatThrownBy(() -> evaluator.evaluate(state, input))
        .isInstanceOfSatisfying(
            ConstitutionAmendmentEvaluationException.class,
            exception -> {
              assertThat(exception.kind())
                  .isEqualTo(ConstitutionAmendmentViolationKind.MISSING_REFERENCE);
              assertThat(exception.code()).isEqualTo("power_not_found");
            });
  }

  @Test
  void rejectsCreatingAnOfficeThatAlreadyExists() {
    UUID jurisdictionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    ConstitutionAmendmentState state =
        state(jurisdictionId, institutionId, ProcedureElectorate.ACTIVE_MEMBERS, null, Map.of());
    CreateConstitutionAmendmentMotionInput input =
        new CreateConstitutionAmendmentMotionInput(
            "Duplicate steward",
            "Create an existing office.",
            List.of(),
            List.of(),
            List.of(
                new CreateOfficeChangeInput(
                    ConstitutionChangeOperation.CREATE,
                    "steward",
                    jurisdictionId,
                    "Steward",
                    "Executive office",
                    30,
                    1)),
            List.of());

    assertThatThrownBy(() -> evaluator.evaluate(state, input))
        .isInstanceOfSatisfying(
            ConstitutionAmendmentEvaluationException.class,
            exception -> {
              assertThat(exception.kind())
                  .isEqualTo(ConstitutionAmendmentViolationKind.CONFLICTING_STATE);
              assertThat(exception.code()).isEqualTo("office_already_exists");
            });
  }

  @Test
  void allowsCoordinatedOfficeRetirementAndReferenceReassignment() {
    UUID jurisdictionId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    ConstitutionAmendmentState state =
        state(
            jurisdictionId, institutionId, ProcedureElectorate.OFFICE_HOLDERS, "steward", Map.of());
    CreateConstitutionAmendmentMotionInput input =
        new CreateConstitutionAmendmentMotionInput(
            "Retire steward",
            "Return authority to citizens.",
            List.of(),
            List.of(
                new CreateProcedureChangeInput(
                    "ordinary-resolution",
                    null,
                    null,
                    null,
                    null,
                    null,
                    ProcedureElectorate.ACTIVE_MEMBERS,
                    null,
                    null,
                    null,
                    null)),
            List.of(
                new CreateOfficeChangeInput(
                    ConstitutionChangeOperation.RETIRE, "steward", null, null, null, null, null)),
            List.of(
                new CreatePowerChangeInput(
                    PowerCode.REQUEST_CERTIFICATION, PowerHolderScope.ACTIVE_MEMBER, null)));

    var plan = evaluator.evaluate(state, input);

    assertThat(plan.officeChanges()).singleElement().extracting("code").isEqualTo("steward");
    assertThat(plan.procedureChanges())
        .singleElement()
        .extracting("electorate")
        .isEqualTo(ProcedureElectorate.ACTIVE_MEMBERS);
    assertThat(plan.powerChanges())
        .singleElement()
        .extracting("holderScope")
        .isEqualTo(PowerHolderScope.ACTIVE_MEMBER);
  }

  private ConstitutionAmendmentState state(
      UUID jurisdictionId,
      UUID institutionId,
      ProcedureElectorate electorate,
      String electorateOfficeCode,
      Map<String, Long> activeTerms) {
    return new ConstitutionAmendmentState(
        Map.of(institutionId, InstitutionKind.ASSEMBLY),
        Set.of("steward"),
        Map.of(
            "ordinary-resolution",
            new ConstitutionAmendmentProcedureState(
                institutionId,
                EffectType.ADOPT_RESOLUTION,
                VotingThreshold.SIMPLE_MAJORITY_CAST,
                null,
                electorate,
                electorateOfficeCode)),
        Map.of(
            PowerCode.REQUEST_CERTIFICATION,
            new ConstitutionAmendmentPowerState(PowerHolderScope.OFFICE, "steward")),
        Set.of(jurisdictionId),
        activeTerms);
  }
}
