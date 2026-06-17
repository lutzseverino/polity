package com.odonta.polity.model;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class ValidProcedureChangeValidator
    implements ConstraintValidator<ValidProcedureChange, CreateProcedureChangeInput> {

  @Override
  public boolean isValid(CreateProcedureChangeInput value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    boolean quorumAbsent = value.quorumNumerator() == null && value.quorumDenominator() == null;
    boolean quorumComplete = value.quorumNumerator() != null && value.quorumDenominator() != null;
    boolean quorumValid =
        quorumAbsent || (quorumComplete && value.quorumNumerator() <= value.quorumDenominator());
    return quorumValid
        && (quorumComplete
            || value.threshold() != null
            || value.minimumNoticeHours() != null
            || value.votingPeriodHours() != null);
  }
}
