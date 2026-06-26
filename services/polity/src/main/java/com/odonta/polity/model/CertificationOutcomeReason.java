package com.odonta.polity.model;

public enum CertificationOutcomeReason {
  PASSED,
  QUORUM_NOT_MET,
  THRESHOLD_NOT_MET,
  NO_DECISIVE_PLURALITY;

  public String labelKey() {
    return "certification.outcome_reason." + name().toLowerCase();
  }
}
