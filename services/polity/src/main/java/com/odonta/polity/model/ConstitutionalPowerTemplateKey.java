package com.odonta.polity.model;

public enum ConstitutionalPowerTemplateKey {
  ADMIT_MEMBER("constitutional_power.admit_member"),
  INTRODUCE_AMENDMENT("constitutional_power.introduce_amendment"),
  INTRODUCE_APPEAL("constitutional_power.introduce_appeal"),
  INTRODUCE_CONSTITUTIONAL_REVIEW("constitutional_power.introduce_constitutional_review"),
  INTRODUCE_DISBANDMENT("constitutional_power.introduce_disbandment"),
  INTRODUCE_OFFICE_TERM_REVIEW("constitutional_power.introduce_office_term_review"),
  INTRODUCE_MOTION("constitutional_power.introduce_motion"),
  INTRODUCE_OFFICE_ELECTION("constitutional_power.introduce_office_election"),
  INTRODUCE_SANCTION("constitutional_power.introduce_sanction"),
  REQUEST_CERTIFICATION("constitutional_power.request_certification");

  private final String keyPrefix;

  ConstitutionalPowerTemplateKey(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public String nameKey() {
    return keyPrefix + ".name";
  }

  public String storedName() {
    return nameKey();
  }
}
