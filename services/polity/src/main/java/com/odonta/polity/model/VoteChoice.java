package com.odonta.polity.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum VoteChoice {
  YES("yes"),
  NO("no"),
  ABSTAIN("abstain");

  private final String wireValue;

  VoteChoice(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String wireValue() {
    return wireValue;
  }

  @JsonCreator
  public static VoteChoice fromWireValue(String value) {
    return Arrays.stream(values())
        .filter(choice -> choice.wireValue.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown vote choice: " + value));
  }
}
