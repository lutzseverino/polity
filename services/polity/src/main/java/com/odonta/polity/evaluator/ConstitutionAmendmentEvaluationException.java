package com.odonta.polity.evaluator;

public final class ConstitutionAmendmentEvaluationException extends RuntimeException {
  private final ConstitutionAmendmentViolationKind kind;
  private final String code;

  public ConstitutionAmendmentEvaluationException(
      ConstitutionAmendmentViolationKind kind, String code, String message) {
    super(message);
    this.kind = kind;
    this.code = code;
  }

  public ConstitutionAmendmentViolationKind kind() {
    return kind;
  }

  public String code() {
    return code;
  }
}
