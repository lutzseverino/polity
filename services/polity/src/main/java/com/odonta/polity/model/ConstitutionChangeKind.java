package com.odonta.polity.model;

public enum ConstitutionChangeKind {
  INSTITUTION("institution"),
  OFFICE("office"),
  POWER("power"),
  PROCEDURE("procedure");

  private final String value;

  ConstitutionChangeKind(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
