package com.odonta.polity.exception;

import com.odonta.common.api.ApiException;

public enum PolityResource {
  MEMBER("member_not_found", "Member not found."),
  OFFICE("office_not_found", "Office not found."),
  POLITY("polity_not_found", "Polity not found."),
  CONSTITUTION("constitution_not_found", "Constitution not found."),
  JURISDICTION("jurisdiction_not_found", "Jurisdiction not found."),
  INSTITUTION("institution_not_found", "Institution not found."),
  MOTION("motion_not_found", "Motion not found."),
  PROCEDURE("procedure_not_found", "Procedure not found."),
  OFFICE_TERM("office_term_not_found", "Office term not found."),
  SANCTION("sanction_not_found", "Sanction not found."),
  INVITATION("invitation_not_found", "Invitation not found."),
  CONSTITUTIONAL_POWER("power_not_found", "Constitutional power not found."),
  OFFICIAL_RECORD_ENTRY("official_record_entry_not_found", "Official record entry not found.");

  private final String notFoundCode;
  private final String notFoundMessage;

  PolityResource(String notFoundCode, String notFoundMessage) {
    this.notFoundCode = notFoundCode;
    this.notFoundMessage = notFoundMessage;
  }

  public String notFoundCode() {
    return notFoundCode;
  }

  public String notFoundMessage() {
    return notFoundMessage;
  }

  public ApiException notFound() {
    return ApiException.notFound(notFoundCode, notFoundMessage);
  }
}
