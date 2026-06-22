package com.odonta.polity.model;

import java.util.UUID;

public record ConstitutionInstitutionResult(
    UUID id, String name, String nameKey, InstitutionKind kind) {}
