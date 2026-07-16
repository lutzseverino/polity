package com.odonta.polity.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.input.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.input.CreateInstitutionChangeInput;
import com.odonta.polity.input.CreateOfficeChangeInput;
import com.odonta.polity.input.CreatePowerChangeInput;
import com.odonta.polity.input.CreateProcedureChangeInput;
import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.junit.jupiter.api.Test;

class ConstitutionAmendmentInputValidationTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  private final Validator messageValidator =
      Validation.byDefaultProvider()
          .configure()
          .messageInterpolator(
              new ResourceBundleMessageInterpolator(new PlatformResourceBundleLocator("messages")))
          .buildValidatorFactory()
          .getValidator();

  @Test
  void rejectsAmendmentWithoutChanges() {
    CreateConstitutionAmendmentMotionInput input =
        new CreateConstitutionAmendmentMotionInput("Title", "Body", null, null, null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation()
                    instanceof ValidConstitutionAmendment);
  }

  @Test
  void acceptsInstitutionCreationWithRequiredFields() {
    CreateInstitutionChangeInput input =
        new CreateInstitutionChangeInput(
            ConstitutionChangeOperation.CREATE,
            null,
            UUID.randomUUID(),
            "People's Court",
            InstitutionKind.JUDICIARY);

    assertThat(validator.validate(input)).isEmpty();
  }

  @Test
  void rejectsInstitutionCreationWithExistingInstitutionId() {
    CreateInstitutionChangeInput input =
        new CreateInstitutionChangeInput(
            ConstitutionChangeOperation.CREATE,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "People's Court",
            InstitutionKind.JUDICIARY);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation()
                    instanceof ValidInstitutionChange);
  }

  @Test
  void rejectsEmptyInstitutionRevision() {
    CreateInstitutionChangeInput input =
        new CreateInstitutionChangeInput(
            ConstitutionChangeOperation.REVISE, UUID.randomUUID(), null, " ", null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation()
                    instanceof ValidInstitutionChange);
  }

  @Test
  void rejectsInstitutionRetirementWithRevisionFields() {
    CreateInstitutionChangeInput input =
        new CreateInstitutionChangeInput(
            ConstitutionChangeOperation.RETIRE, UUID.randomUUID(), null, "New name", null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation()
                    instanceof ValidInstitutionChange);
  }

  @Test
  void rejectsDuplicateAmendmentChanges() {
    CreateConstitutionAmendmentMotionInput input =
        new CreateConstitutionAmendmentMotionInput(
            "Title",
            "Body",
            List.of(
                new CreateProcedureChangeInput(
                    Procedure.ORDINARY_RESOLUTION,
                    null,
                    null,
                    VotingThreshold.SIMPLE_MAJORITY_CAST,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null),
                new CreateProcedureChangeInput(
                    Procedure.ORDINARY_RESOLUTION,
                    null,
                    null,
                    VotingThreshold.TWO_THIRDS_ELIGIBLE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null)),
            null,
            null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation()
                    instanceof ValidConstitutionAmendment);
  }

  @Test
  void acceptsOfficeHeldProcedureElectorateWithOfficeCode() {
    CreateProcedureChangeInput input =
        new CreateProcedureChangeInput(
            Procedure.APPEAL,
            null,
            null,
            null,
            null,
            ProcedureElectorate.OFFICE_HOLDERS,
            "magistrate",
            null,
            null,
            null);

    assertThat(validator.validate(input)).isEmpty();
  }

  @Test
  void rejectsOfficeHeldProcedureElectorateWithoutOfficeCode() {
    CreateProcedureChangeInput input =
        new CreateProcedureChangeInput(
            Procedure.APPEAL,
            null,
            null,
            null,
            null,
            ProcedureElectorate.OFFICE_HOLDERS,
            null,
            null,
            null,
            null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation()
                    instanceof ValidProcedureChange);
  }

  @Test
  void rejectsActiveMemberProcedureElectorateWithOfficeCode() {
    CreateProcedureChangeInput input =
        new CreateProcedureChangeInput(
            Procedure.APPEAL,
            null,
            null,
            null,
            null,
            ProcedureElectorate.ACTIVE_MEMBERS,
            "magistrate",
            null,
            null,
            null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation()
                    instanceof ValidProcedureChange);
  }

  @Test
  void acceptsOfficeCreationWithRequiredFields() {
    CreateOfficeChangeInput input =
        new CreateOfficeChangeInput(
            ConstitutionChangeOperation.CREATE, "clerk", "Clerk", "Keeps records.", 30, 1);

    assertThat(validator.validate(input)).isEmpty();
  }

  @Test
  void rejectsOfficeCreationWithoutRequiredFields() {
    CreateOfficeChangeInput input =
        new CreateOfficeChangeInput(
            ConstitutionChangeOperation.CREATE, "clerk", "", null, null, null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation() instanceof ValidOfficeChange);
  }

  @Test
  void interpolatesCustomValidationMessagesFromMessagesBundle() {
    CreateOfficeChangeInput input =
        new CreateOfficeChangeInput(
            ConstitutionChangeOperation.CREATE, "clerk", "", null, null, null);

    assertThat(messageValidator.validate(input))
        .anySatisfy(
            violation ->
                assertThat(violation.getMessage())
                    .isEqualTo("Office change fields are inconsistent with the requested action."));
  }

  @Test
  void rejectsEmptyOfficeRevision() {
    CreateOfficeChangeInput input =
        new CreateOfficeChangeInput(
            ConstitutionChangeOperation.REVISE, "clerk", null, " ", null, null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation() instanceof ValidOfficeChange);
  }

  @Test
  void rejectsOfficeRetirementWithRevisionFields() {
    CreateOfficeChangeInput input =
        new CreateOfficeChangeInput(
            ConstitutionChangeOperation.RETIRE, "clerk", "Clerk", null, null, null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation() instanceof ValidOfficeChange);
  }

  @Test
  void acceptsOfficeHeldPowerWithOfficeCode() {
    CreatePowerChangeInput input =
        new CreatePowerChangeInput(PowerCode.ADMIT_MEMBER, PowerHolderScope.OFFICE, "clerk");

    assertThat(validator.validate(input)).isEmpty();
  }

  @Test
  void rejectsOfficeHeldPowerWithoutOfficeCode() {
    CreatePowerChangeInput input =
        new CreatePowerChangeInput(PowerCode.ADMIT_MEMBER, PowerHolderScope.OFFICE, null);

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation() instanceof ValidPowerChange);
  }

  @Test
  void rejectsDisbandmentPowerHeldByOffice() {
    CreatePowerChangeInput input =
        new CreatePowerChangeInput(
            PowerCode.INTRODUCE_DISBANDMENT, PowerHolderScope.OFFICE, "steward");

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation() instanceof ValidPowerChange);
  }

  @Test
  void rejectsCitizenHeldPowerWithOfficeCode() {
    CreatePowerChangeInput input =
        new CreatePowerChangeInput(
            PowerCode.INTRODUCE_MOTION, PowerHolderScope.ACTIVE_MEMBER, "steward");

    assertThat(validator.validate(input))
        .anyMatch(
            violation ->
                violation.getConstraintDescriptor().getAnnotation() instanceof ValidPowerChange);
  }
}
