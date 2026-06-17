package com.odonta.polity.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateConstitutionAmendmentMotionInput(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 10000) String body,
    @Valid @NotEmpty @Size(max = 10) List<CreateProcedureChangeInput> procedureChanges) {}
