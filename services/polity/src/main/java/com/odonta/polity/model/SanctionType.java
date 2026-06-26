package com.odonta.polity.model;

public enum SanctionType {
  WARNING,
  SUSPENSION;

  public String labelKey() {
    return "sanction.type." + name().toLowerCase();
  }
}
