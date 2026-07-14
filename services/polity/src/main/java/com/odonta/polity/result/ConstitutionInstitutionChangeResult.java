package com.odonta.polity.result;

import com.odonta.polity.model.ConstitutionInstitutionChangeAction;
import com.odonta.polity.model.InstitutionKind;
import java.util.UUID;

public record ConstitutionInstitutionChangeResult(
    ConstitutionInstitutionChangeAction action,
    UUID institutionId,
    UUID jurisdictionId,
    String name,
    InstitutionKind kind) {}
