package com.odonta.polity.result;

import java.util.List;

public record ConstitutionAmendmentProposalResult(
    String title,
    String body,
    List<ConstitutionInstitutionChangeResult> institutionChanges,
    List<ConstitutionProcedureChangeResult> procedureChanges,
    List<ConstitutionOfficeChangeResult> officeChanges,
    List<ConstitutionPowerChangeResult> powerChanges) {
  public ConstitutionAmendmentProposalResult {
    institutionChanges = List.copyOf(institutionChanges);
    procedureChanges = List.copyOf(procedureChanges);
    officeChanges = List.copyOf(officeChanges);
    powerChanges = List.copyOf(powerChanges);
  }
}
