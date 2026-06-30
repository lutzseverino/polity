package com.odonta.polity.model;

public enum PowerCode {
  ADMIT_MEMBER(false),
  INTRODUCE_MOTION(false),
  INTRODUCE_OFFICE_ELECTION(false),
  INTRODUCE_SANCTION(false),
  INTRODUCE_APPEAL(false),
  INTRODUCE_OFFICE_TERM_REVIEW(false),
  INTRODUCE_CONSTITUTIONAL_REVIEW(false),
  INTRODUCE_AMENDMENT(false),
  INTRODUCE_DISBANDMENT(true),
  REQUEST_CERTIFICATION(false);

  private final boolean activeMemberHolderRequired;

  PowerCode(boolean activeMemberHolderRequired) {
    this.activeMemberHolderRequired = activeMemberHolderRequired;
  }

  public boolean requiresActiveMemberHolder() {
    return activeMemberHolderRequired;
  }
}
