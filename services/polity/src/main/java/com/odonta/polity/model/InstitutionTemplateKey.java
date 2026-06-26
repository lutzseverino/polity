package com.odonta.polity.model;

public enum InstitutionTemplateKey {
  CITIZENS_ASSEMBLY("polity.institution.citizensAssembly"),
  MAGISTRATES_COURT("polity.institution.magistratesCourt");

  private final String keyPrefix;

  InstitutionTemplateKey(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public String nameKey() {
    return keyPrefix + ".name";
  }

  public String storedName() {
    return nameKey();
  }
}
