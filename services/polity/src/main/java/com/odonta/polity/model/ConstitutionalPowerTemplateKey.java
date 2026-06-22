package com.odonta.polity.model;

public enum ConstitutionalPowerTemplateKey {
  ADMIT_MEMBER("constitutional_power.admit_member", "Admit citizens"),
  INTRODUCE_AMENDMENT(
      "constitutional_power.introduce_amendment", "Propose constitutional amendments"),
  INTRODUCE_APPEAL("constitutional_power.introduce_appeal", "Propose appeals"),
  INTRODUCE_DISBANDMENT("constitutional_power.introduce_disbandment", "Propose disbandment"),
  INTRODUCE_MOTION("constitutional_power.introduce_motion", "Introduce resolutions"),
  INTRODUCE_OFFICE_ELECTION(
      "constitutional_power.introduce_office_election", "Propose office elections"),
  INTRODUCE_SANCTION("constitutional_power.introduce_sanction", "Propose sanctions"),
  REQUEST_CERTIFICATION("constitutional_power.request_certification", "Request certification");

  private final String keyPrefix;
  private final String fallbackName;

  ConstitutionalPowerTemplateKey(String keyPrefix, String fallbackName) {
    this.keyPrefix = keyPrefix;
    this.fallbackName = fallbackName;
  }

  public String nameKey() {
    return keyPrefix + ".name";
  }

  public String fallbackName() {
    return fallbackName;
  }
}
