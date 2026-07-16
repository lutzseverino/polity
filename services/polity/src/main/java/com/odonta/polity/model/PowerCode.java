package com.odonta.polity.model;

public enum PowerCode {
  ADMIT_MEMBER(false, "constitutional_power.admit_member"),
  INTRODUCE_MOTION(false, "constitutional_power.introduce_motion"),
  INTRODUCE_OFFICE_ELECTION(false, "constitutional_power.introduce_office_election"),
  INTRODUCE_SANCTION(false, "constitutional_power.introduce_sanction"),
  INTRODUCE_APPEAL(false, "constitutional_power.introduce_appeal"),
  INTRODUCE_OFFICE_TERM_REVIEW(false, "constitutional_power.introduce_office_term_review"),
  INTRODUCE_CONSTITUTIONAL_REVIEW(false, "constitutional_power.introduce_constitutional_review"),
  INTRODUCE_AMENDMENT(false, "constitutional_power.introduce_amendment"),
  INTRODUCE_DISBANDMENT(true, "constitutional_power.introduce_disbandment"),
  REQUEST_CERTIFICATION(false, "constitutional_power.request_certification");

  private final boolean activeMemberHolderRequired;
  private final String defaultNameKey;

  PowerCode(boolean activeMemberHolderRequired, String defaultNameKeyPrefix) {
    this.activeMemberHolderRequired = activeMemberHolderRequired;
    this.defaultNameKey = defaultNameKeyPrefix + ".name";
  }

  public boolean requiresActiveMemberHolder() {
    return activeMemberHolderRequired;
  }

  public String defaultNameKey() {
    return defaultNameKey;
  }

  public String defaultStoredName() {
    return defaultNameKey;
  }
}
