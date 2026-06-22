package com.odonta.polity.validation;

import com.odonta.polity.model.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.model.CreateOfficeChangeInput;
import com.odonta.polity.model.CreatePowerChangeInput;
import com.odonta.polity.model.CreateProcedureChangeInput;
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
    List<CreateOfficeChangeInput> officeChanges = list(value.officeChanges());
    List<CreatePowerChangeInput> powerChanges = list(value.powerChanges());
    return !(procedureChanges.isEmpty() && officeChanges.isEmpty() && powerChanges.isEmpty())
        && hasUniqueProcedureCodes(procedureChanges)
        && hasUniqueOfficeCodes(officeChanges)
        && hasUniquePowerCodes(powerChanges);
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
