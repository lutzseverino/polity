package com.odonta.polity.model;

import java.util.Map;

public enum OfficialRecordTemplateKey {
  APPEAL_GRANTED("official_record.appeal_granted"),
  BOOTSTRAP_STEWARD_ASSIGNED("official_record.bootstrap_steward_assigned"),
  CANDIDACY_ACCEPTED("official_record.candidacy_accepted"),
  CANDIDACY_DECLINED("official_record.candidacy_declined"),
  CONSTITUTION_AMENDED("official_record.constitution_amended"),
  CONSTITUTION_RATIFIED("official_record.constitution_ratified"),
  MEMBER_ADMITTED("official_record.member_admitted"),
  MEMBER_INVITED("official_record.member_invited"),
  MEMBER_RESIGNED("official_record.member_resigned"),
  MOTION_CERTIFIED("official_record.motion_certified"),
  MOTION_INTRODUCED("official_record.motion_introduced"),
  MOTION_REJECTED("official_record.motion_rejected"),
  OFFICE_ASSIGNED("official_record.office_assigned"),
  OFFICE_ELECTED("official_record.office_elected"),
  OFFICE_TERM_VACATED("official_record.office_term_vacated"),
  OFFICIAL_ACT_VOIDED("official_record.official_act_voided"),
  OFFICE_ELECTION_BALLOT_CAST("official_record.office_election_ballot_cast"),
  POLITY_DISBANDED("official_record.polity_disbanded"),
  POLITY_DISBANDED_BY_LAST_RESIGNATION("official_record.polity_disbanded_by_last_resignation"),
  POLITY_FOUNDED("official_record.polity_founded"),
  RESOLUTION_ADOPTED("official_record.resolution_adopted"),
  SANCTION_APPLIED("official_record.sanction_applied"),
  VOTE_CAST("official_record.vote_cast");

  private final String keyPrefix;

  OfficialRecordTemplateKey(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public String titleKey() {
    return keyPrefix + ".title";
  }

  public String bodyKey() {
    return keyPrefix + ".body";
  }

  public String storedTitle(Map<String, ?> params) {
    return titleKey();
  }

  public String storedBody(Map<String, ?> params) {
    return bodyKey();
  }
}
