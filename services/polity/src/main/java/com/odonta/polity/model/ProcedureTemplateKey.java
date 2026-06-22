package com.odonta.polity.model;

public enum ProcedureTemplateKey {
  APPEAL("procedure.appeal", "Appeal"),
  CONSTITUTION_AMENDMENT("procedure.constitution_amendment", "Constitutional amendment"),
  DISBANDMENT("procedure.disbandment", "Disbandment"),
  OFFICE_ELECTION("procedure.office_election", "Office election"),
  ORDINARY_RESOLUTION("procedure.ordinary_resolution", "Ordinary resolution"),
  SANCTION("procedure.sanction", "Sanction");

  private final String keyPrefix;
  private final String fallbackName;

  ProcedureTemplateKey(String keyPrefix, String fallbackName) {
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
