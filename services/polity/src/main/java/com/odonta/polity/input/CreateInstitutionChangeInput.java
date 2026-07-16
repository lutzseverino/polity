package com.odonta.polity.input;

import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.model.InstitutionKind;
import com.odonta.polity.validation.ValidInstitutionChange;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@ValidInstitutionChange
public record CreateInstitutionChangeInput(
    @NotNull ConstitutionChangeOperation action,
    UUID institutionId,
    UUID jurisdictionId,
    @Size(max = 120) String name,
    InstitutionKind kind) {}
