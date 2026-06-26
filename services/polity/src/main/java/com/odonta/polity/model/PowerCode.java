package com.odonta.polity.model;

public enum PowerCode {
  ADMIT_MEMBER(false),
  INTRODUCE_MOTION(true),
  INTRODUCE_OFFICE_ELECTION(true),
  INTRODUCE_SANCTION(false),
  INTRODUCE_APPEAL(true),
  INTRODUCE_OFFICE_TERM_REVIEW(true),
  INTRODUCE_CONSTITUTIONAL_REVIEW(true),
  INTRODUCE_AMENDMENT(true),
  INTRODUCE_DISBANDMENT(true),
  REQUEST_CERTIFICATION(true);

  private final boolean activeMemberHolderRequired;

  PowerCode(boolean activeMemberHolderRequired) {
    this.activeMemberHolderRequired = activeMemberHolderRequired;
  }

  public boolean requiresActiveMemberHolder() {
    return activeMemberHolderRequired;
  }
}
