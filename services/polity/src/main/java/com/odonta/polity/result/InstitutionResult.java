package com.odonta.polity.result;

import com.odonta.polity.model.InstitutionKind;
import java.util.UUID;

public record InstitutionResult(
    UUID id, UUID jurisdictionId, String name, String nameKey, InstitutionKind kind) {}
