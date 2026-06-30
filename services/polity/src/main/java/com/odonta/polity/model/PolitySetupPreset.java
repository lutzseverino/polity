package com.odonta.polity.model;

public enum PolitySetupPreset {
  STANDARD_CONSTITUTIONAL_COUNCIL_REPUBLIC;

  public String labelKey() {
    return "polity.setup_preset." + name().toLowerCase();
  }
}
