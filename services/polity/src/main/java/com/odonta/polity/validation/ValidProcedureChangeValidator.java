package com.odonta.polity.validation;

import com.odonta.polity.model.CreateProcedureChangeInput;
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
    boolean electorateValid =
        value.electorate() == null
            ? isBlank(value.electorateOfficeCode())
            : switch (value.electorate()) {
              case ACTIVE_MEMBERS -> isBlank(value.electorateOfficeCode());
              case OFFICE_HOLDERS -> !isBlank(value.electorateOfficeCode());
            };
    return quorumValid
        && electorateValid
        && (value.institutionId() != null
            || quorumComplete
            || value.threshold() != null
            || value.electorate() != null
            || value.minimumElectorCount() != null
            || value.minimumNoticeHours() != null
            || value.votingPeriodHours() != null);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
