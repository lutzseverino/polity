package com.odonta.polity.model;

public enum ConstitutionChangeOperation {
  CREATE("create"),
  RETIRE("retire"),
  REVISE("revise");

  private final String value;

  ConstitutionChangeOperation(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
