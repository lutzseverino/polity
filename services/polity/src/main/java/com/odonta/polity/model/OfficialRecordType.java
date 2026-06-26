package com.odonta.polity.model;

public enum OfficialRecordType {
  POLITY_FOUNDED("polity_founded"),
  CONSTITUTION_RATIFIED("constitution_ratified"),
  MEMBER_INVITED("member_invited"),
  MEMBER_ADMITTED("member_admitted"),
  MOTION_INTRODUCED("motion_introduced"),
  CANDIDACY_RESPONDED("candidacy_responded"),
  VOTE_CAST("vote_cast"),
  MOTION_CERTIFIED("motion_certified"),
  RESOLUTION_ADOPTED("resolution_adopted"),
  OFFICE_ASSIGNED("office_assigned"),
  OFFICE_ELECTED("office_elected"),
  OFFICE_TERM_VACATED("office_term_vacated"),
  SANCTION_APPLIED("sanction_applied"),
  APPEAL_GRANTED("appeal_granted"),
  OFFICIAL_ACT_VOIDED("official_act_voided"),
  CONSTITUTION_AMENDED("constitution_amended"),
  POLITY_DISBANDED("polity_disbanded"),
  MOTION_REJECTED("motion_rejected");

  private final String wireValue;

  OfficialRecordType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public String labelKey() {
    return "official_record.type." + wireValue;
  }

  public String storedLabel() {
    return labelKey();
  }

  public boolean isConstitutionallyReviewable() {
    return this == RESOLUTION_ADOPTED || this == SANCTION_APPLIED || this == OFFICE_ELECTED;
  }
}
