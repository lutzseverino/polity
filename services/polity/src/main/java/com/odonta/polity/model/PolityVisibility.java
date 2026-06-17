package com.odonta.polity.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum PolityVisibility {
  PUBLIC("public"),
  PRIVATE("private");

  private final String wireValue;

  PolityVisibility(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String wireValue() {
    return wireValue;
  }

  @JsonCreator
  public static PolityVisibility fromWireValue(String value) {
    return Arrays.stream(values())
        .filter(visibility -> visibility.wireValue.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown polity visibility: " + value));
  }
}
