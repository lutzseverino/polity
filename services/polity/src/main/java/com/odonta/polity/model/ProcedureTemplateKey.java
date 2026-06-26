package com.odonta.polity.model;

public enum ProcedureTemplateKey {
  APPEAL("procedure.appeal"),
  CONSTITUTION_AMENDMENT("procedure.constitution_amendment"),
  CONSTITUTIONAL_REVIEW("procedure.constitutional_review"),
  DISBANDMENT("procedure.disbandment"),
  OFFICE_TERM_REVIEW("procedure.office_term_review"),
  OFFICE_ELECTION("procedure.office_election"),
  ORDINARY_RESOLUTION("procedure.ordinary_resolution"),
  SANCTION("procedure.sanction");

  private final String keyPrefix;

  ProcedureTemplateKey(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public String nameKey() {
    return keyPrefix + ".name";
  }

  public String storedName() {
    return nameKey();
  }
}
