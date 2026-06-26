package com.odonta.polity.model;

import java.util.UUID;

public record ConstitutionInstitutionResult(
    UUID id, UUID jurisdictionId, String name, String nameKey, InstitutionKind kind) {}
