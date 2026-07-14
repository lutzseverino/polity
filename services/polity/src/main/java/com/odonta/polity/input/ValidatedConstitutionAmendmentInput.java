package com.odonta.polity.input;

import java.util.List;

public record ValidatedConstitutionAmendmentInput(
    List<CreateInstitutionChangeInput> institutionChanges,
    List<CreateProcedureChangeInput> procedureChanges,
    List<CreateOfficeChangeInput> officeChanges,
    List<CreatePowerChangeInput> powerChanges) {
  public ValidatedConstitutionAmendmentInput {
    institutionChanges = List.copyOf(institutionChanges);
    procedureChanges = List.copyOf(procedureChanges);
    officeChanges = List.copyOf(officeChanges);
    powerChanges = List.copyOf(powerChanges);
  }
}
