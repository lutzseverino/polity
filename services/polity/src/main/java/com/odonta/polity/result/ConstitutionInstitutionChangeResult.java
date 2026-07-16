package com.odonta.polity.result;

import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.model.InstitutionKind;
import java.util.UUID;

public record ConstitutionInstitutionChangeResult(
    ConstitutionChangeOperation action,
    UUID institutionId,
    UUID jurisdictionId,
    String name,
    InstitutionKind kind) {}
