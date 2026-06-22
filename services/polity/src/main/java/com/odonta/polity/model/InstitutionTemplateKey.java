package com.odonta.polity.model;

public enum InstitutionTemplateKey {
  CITIZENS_ASSEMBLY("polity.institution.citizensAssembly", "Citizens' Assembly");

  private final String keyPrefix;
  private final String fallbackName;

  InstitutionTemplateKey(String keyPrefix, String fallbackName) {
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
