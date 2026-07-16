package com.odonta.polity.result;

import java.util.Arrays;

public enum ActionUnavailableReason {
  APPEAL_PROCEDURE_UNAVAILABLE("appeal_procedure_unavailable"),
  CANDIDACY_NOT_FOUND("candidacy_not_found"),
  CANDIDACY_RESPONSE_CLOSED("candidacy_response_closed"),
  CERTIFICATION_NOT_OPEN("certification_not_open"),
  CONSTITUTION_SUPERSEDED("constitution_superseded"),
  CONSTITUTIONAL_AUTHORITY_MISSING("constitutional_authority_missing"),
  CONSTITUTIONAL_OFFICE_VACANT("constitutional_office_vacant"),
  CONSTITUTIONAL_POWER_MISSING("constitutional_power_missing"),
  LAST_MEMBER_RESIGNATION_UNAVAILABLE("last_member_resignation_unavailable"),
  MOTION_NOT_OFFICE_ELECTION("motion_not_office_election"),
  MOTION_NOT_VOTING("motion_not_voting"),
  OFFICE_ELECTION_BALLOT_REQUIRED("office_election_ballot_required"),
  POLITICAL_STANDING_REQUIRED("political_standing_required"),
  POLITY_DISBANDED("polity_disbanded"),
  POLITY_MEMBERSHIP_REQUIRED("polity_membership_required"),
  PROCEDURE_ELECTORATE_BELOW_MINIMUM("procedure_electorate_below_minimum"),
  PROCEDURE_ELECTORATE_EMPTY("procedure_electorate_empty"),
  PROCEDURE_ELECTORATE_OFFICE_VACANT("procedure_electorate_office_vacant"),
  PROCEDURE_MISSING("procedure_missing"),
  PROVISIONAL_FOUNDER_RESIGNATION_UNAVAILABLE("provisional_founder_resignation_unavailable"),
  VOTE_INELIGIBLE("vote_ineligible"),
  VOTING_CLOSED("voting_closed"),
  VOTING_NOT_OPEN("voting_not_open");

  private final String wireValue;

  ActionUnavailableReason(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static ActionUnavailableReason fromWireValue(String wireValue) {
    return Arrays.stream(values())
        .filter(reason -> reason.wireValue.equals(wireValue))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unknown action-unavailable reason: " + wireValue));
  }
}
