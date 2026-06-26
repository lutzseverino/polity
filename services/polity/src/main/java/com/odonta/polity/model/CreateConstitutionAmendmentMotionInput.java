package com.odonta.polity.model;

import com.odonta.polity.validation.ValidConstitutionAmendment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@ValidConstitutionAmendment
public record CreateConstitutionAmendmentMotionInput(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 10000) String body,
    @Valid @Size(max = 10) List<CreateInstitutionChangeInput> institutionChanges,
    @Valid @Size(max = 10) List<CreateProcedureChangeInput> procedureChanges,
    @Valid @Size(max = 10) List<CreateOfficeChangeInput> officeChanges,
    @Valid @Size(max = 10) List<CreatePowerChangeInput> powerChanges) {
  public CreateConstitutionAmendmentMotionInput(
      String title,
      String body,
      List<CreateProcedureChangeInput> procedureChanges,
      List<CreateOfficeChangeInput> officeChanges,
      List<CreatePowerChangeInput> powerChanges) {
    this(title, body, null, procedureChanges, officeChanges, powerChanges);
  }
}
