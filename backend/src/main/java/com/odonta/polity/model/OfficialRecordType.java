package com.odonta.polity.model;

public enum OfficialRecordType {
  POLITY_FOUNDED("polity_founded"),
  CONSTITUTION_RATIFIED("constitution_ratified"),
  MEMBER_ADMITTED("member_admitted"),
  MOTION_INTRODUCED("motion_introduced"),
  VOTE_CAST("vote_cast"),
  MOTION_CERTIFIED("motion_certified"),
  RESOLUTION_ADOPTED("resolution_adopted"),
  MOTION_REJECTED("motion_rejected");

  private final String wireValue;

  OfficialRecordType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
