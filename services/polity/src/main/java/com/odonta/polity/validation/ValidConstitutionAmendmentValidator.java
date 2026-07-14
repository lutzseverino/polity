package com.odonta.polity.validation;

import com.odonta.polity.input.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.input.CreateInstitutionChangeInput;
import com.odonta.polity.input.CreateOfficeChangeInput;
import com.odonta.polity.input.CreatePowerChangeInput;
import com.odonta.polity.input.CreateProcedureChangeInput;
import com.odonta.polity.model.PowerCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ValidConstitutionAmendmentValidator
    implements ConstraintValidator<
        ValidConstitutionAmendment, CreateConstitutionAmendmentMotionInput> {

  @Override
  public boolean isValid(
      CreateConstitutionAmendmentMotionInput value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    List<CreateProcedureChangeInput> procedureChanges = list(value.procedureChanges());
    List<CreateInstitutionChangeInput> institutionChanges = list(value.institutionChanges());
    List<CreateOfficeChangeInput> officeChanges = list(value.officeChanges());
    List<CreatePowerChangeInput> powerChanges = list(value.powerChanges());
    return !(institutionChanges.isEmpty()
            && procedureChanges.isEmpty()
            && officeChanges.isEmpty()
            && powerChanges.isEmpty())
        && hasUniqueInstitutionIds(institutionChanges)
        && hasUniqueProcedureCodes(procedureChanges)
        && hasUniqueOfficeCodes(officeChanges)
        && hasUniquePowerCodes(powerChanges);
  }

  private boolean hasUniqueInstitutionIds(List<CreateInstitutionChangeInput> changes) {
    Set<java.util.UUID> ids = new HashSet<>();
    return changes.stream()
        .filter(change -> change.institutionId() != null)
        .allMatch(change -> ids.add(change.institutionId()));
  }

  private boolean hasUniqueProcedureCodes(List<CreateProcedureChangeInput> changes) {
    Set<String> codes = new HashSet<>();
    return changes.stream().allMatch(change -> codes.add(change.procedureCode()));
  }

  private boolean hasUniqueOfficeCodes(List<CreateOfficeChangeInput> changes) {
    Set<String> codes = new HashSet<>();
    return changes.stream().allMatch(change -> codes.add(normalized(change.code())));
  }

  private boolean hasUniquePowerCodes(List<CreatePowerChangeInput> changes) {
    Set<PowerCode> codes = new HashSet<>();
    return changes.stream().allMatch(change -> codes.add(change.powerCode()));
  }

  private <T> List<T> list(List<T> values) {
    return values == null ? List.of() : values;
  }

  private String normalized(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
