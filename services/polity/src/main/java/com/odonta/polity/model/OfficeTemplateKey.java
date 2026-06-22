package com.odonta.polity.model;

public enum OfficeTemplateKey {
  MAGISTRATE(
      "office.magistrate",
      "Magistrate",
      "Decides appeals so sanctions remain reviewable by an independent office."),
  STEWARD("office.steward", "Steward", "Admits citizens and coordinates official proceedings."),
  TRIBUNE(
      "office.tribune",
      "Tribune",
      "Introduces sanction proceedings while citizens retain voting and appeal rights.");

  private final String keyPrefix;
  private final String fallbackName;
  private final String fallbackDescription;

  OfficeTemplateKey(String keyPrefix, String fallbackName, String fallbackDescription) {
    this.keyPrefix = keyPrefix;
    this.fallbackName = fallbackName;
    this.fallbackDescription = fallbackDescription;
  }

  public String nameKey() {
    return keyPrefix + ".name";
  }

  public String descriptionKey() {
    return keyPrefix + ".description";
  }

  public String fallbackName() {
    return fallbackName;
  }

  public String fallbackDescription() {
    return fallbackDescription;
  }
}
