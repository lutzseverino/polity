package com.odonta.polity.validation;

import com.odonta.polity.model.CreatePowerChangeInput;
import com.odonta.polity.model.PowerHolderScope;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class ValidPowerChangeValidator
    implements ConstraintValidator<ValidPowerChange, CreatePowerChangeInput> {

  @Override
  public boolean isValid(CreatePowerChangeInput value, ConstraintValidatorContext context) {
    if (value == null || value.holderScope() == null) {
      return true;
    }
    if (value.powerCode() != null
        && value.powerCode().requiresActiveMemberHolder()
        && value.holderScope() != PowerHolderScope.ACTIVE_MEMBER) {
      return false;
    }
    return switch (value.holderScope()) {
      case OFFICE -> !isBlank(value.holderOfficeCode());
      case ACTIVE_MEMBER -> isBlank(value.holderOfficeCode());
    };
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
