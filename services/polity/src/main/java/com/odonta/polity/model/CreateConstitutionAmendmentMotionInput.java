package com.odonta.polity.model;

import com.odonta.polity.validation.ValidConstitutionAmendment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@ValidConstitutionAmendment
public record CreateConstitutionAmendmentMotionInput(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 10000) String body,
    @Valid @Size(max = 10) List<@NotNull CreateInstitutionChangeInput> institutionChanges,
    @Valid @Size(max = 10) List<@NotNull CreateProcedureChangeInput> procedureChanges,
    @Valid @Size(max = 10) List<@NotNull CreateOfficeChangeInput> officeChanges,
    @Valid @Size(max = 10) List<@NotNull CreatePowerChangeInput> powerChanges) {
  public CreateConstitutionAmendmentMotionInput(
      String title,
      String body,
      List<CreateProcedureChangeInput> procedureChanges,
      List<CreateOfficeChangeInput> officeChanges,
      List<CreatePowerChangeInput> powerChanges) {
    this(title, body, null, procedureChanges, officeChanges, powerChanges);
  }
}
