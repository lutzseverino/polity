package com.odonta.polity.model;

public enum OfficialRecordOutcome {
  ADOPTED("adopted"),
  APPEAL_GRANTED("appeal_granted"),
  CANDIDACY_ACCEPTED("candidacy_accepted"),
  CANDIDACY_DECLINED("candidacy_declined"),
  CONSTITUTION_AMENDED("constitution_amended"),
  ELECTION_BALLOT_CAST("election_ballot_cast"),
  INTRODUCED("introduced"),
  OFFICE_ELECTED("office_elected"),
  OFFICE_TERM_VACATED("office_term_vacated"),
  PASSED("passed"),
  POLITY_DISBANDED("polity_disbanded"),
  REJECTED("rejected"),
  SANCTION_APPLIED("sanction_applied"),
  VOIDED("voided"),
  VOTE_CAST("vote_cast");

  private final String value;

  OfficialRecordOutcome(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
