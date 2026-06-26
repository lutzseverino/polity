package com.odonta.polity.model;

public enum ConstitutionTemplateKey {
  STRUCTURED_CHARTER("constitution.structured_charter");

  private final String keyPrefix;

  ConstitutionTemplateKey(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public String titleKey() {
    return keyPrefix + ".title";
  }

  public String bodyKey() {
    return keyPrefix + ".body";
  }

  public String storedTitle() {
    return titleKey();
  }

  public String storedTitle(int version) {
    return titleKey();
  }

  public String storedBody() {
    return bodyKey();
  }
}
