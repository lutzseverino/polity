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
    @Valid @Size(max = 10) List<CreateProcedureChangeInput> procedureChanges,
    @Valid @Size(max = 10) List<CreateOfficeChangeInput> officeChanges,
    @Valid @Size(max = 10) List<CreatePowerChangeInput> powerChanges) {}
