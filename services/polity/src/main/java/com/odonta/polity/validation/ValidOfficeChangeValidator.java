package com.odonta.polity.validation;

import com.odonta.polity.model.CreateOfficeChangeInput;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class ValidOfficeChangeValidator
    implements ConstraintValidator<ValidOfficeChange, CreateOfficeChangeInput> {

  @Override
  public boolean isValid(CreateOfficeChangeInput value, ConstraintValidatorContext context) {
    if (value == null || value.action() == null) {
      return true;
    }
    return switch (value.action()) {
      case CREATE ->
          hasText(value.name())
              && hasText(value.description())
              && value.termLengthDays() != null
              && value.seatCount() != null;
      case REVISE ->
          value.jurisdictionId() != null
              || hasText(value.name())
              || hasText(value.description())
              || value.termLengthDays() != null
              || value.seatCount() != null;
      case RETIRE ->
          value.jurisdictionId() == null
              && isBlank(value.name())
              && isBlank(value.description())
              && value.termLengthDays() == null
              && value.seatCount() == null;
    };
  }

  private boolean hasText(String value) {
    return !isBlank(value);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
