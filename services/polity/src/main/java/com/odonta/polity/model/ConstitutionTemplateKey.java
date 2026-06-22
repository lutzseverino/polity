package com.odonta.polity.model;

public enum ConstitutionTemplateKey {
  STRUCTURED_CHARTER(
      "constitution.structured_charter",
      "Structured Charter",
      "Binding constitutional rules are stored as structured institutions, procedures, offices, and powers.");

  private final String keyPrefix;
  private final String fallbackTitle;
  private final String fallbackBody;

  ConstitutionTemplateKey(String keyPrefix, String fallbackTitle, String fallbackBody) {
    this.keyPrefix = keyPrefix;
    this.fallbackTitle = fallbackTitle;
    this.fallbackBody = fallbackBody;
  }

  public String titleKey() {
    return keyPrefix + ".title";
  }

  public String bodyKey() {
    return keyPrefix + ".body";
  }

  public String fallbackTitle() {
    return fallbackTitle;
  }

  public String fallbackTitle(int version) {
    return fallbackTitle + " v" + version;
  }

  public String fallbackBody() {
    return fallbackBody;
  }
}
