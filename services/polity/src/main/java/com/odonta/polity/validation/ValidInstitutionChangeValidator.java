package com.odonta.polity.validation;

import com.odonta.polity.input.CreateInstitutionChangeInput;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class ValidInstitutionChangeValidator
    implements ConstraintValidator<ValidInstitutionChange, CreateInstitutionChangeInput> {

  @Override
  public boolean isValid(CreateInstitutionChangeInput value, ConstraintValidatorContext context) {
    if (value == null || value.action() == null) {
      return true;
    }
    return switch (value.action()) {
      case CREATE ->
          value.institutionId() == null
              && value.jurisdictionId() != null
              && hasText(value.name())
              && value.kind() != null;
      case REVISE ->
          value.institutionId() != null
              && (value.jurisdictionId() != null || hasText(value.name()) || value.kind() != null);
      case RETIRE ->
          value.institutionId() != null
              && value.jurisdictionId() == null
              && isBlank(value.name())
              && value.kind() == null;
    };
  }

  private boolean hasText(String value) {
    return !isBlank(value);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
