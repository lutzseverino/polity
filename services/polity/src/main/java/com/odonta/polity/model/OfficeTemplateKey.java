package com.odonta.polity.model;

public enum OfficeTemplateKey {
  COUNCILOR("office.councilor"),
  MAGISTRATE("office.magistrate"),
  STEWARD("office.steward"),
  TRIBUNE("office.tribune");

  private final String keyPrefix;

  OfficeTemplateKey(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public String nameKey() {
    return keyPrefix + ".name";
  }

  public String descriptionKey() {
    return keyPrefix + ".description";
  }

  public String storedName() {
    return nameKey();
  }

  public String storedDescription() {
    return descriptionKey();
  }
}
