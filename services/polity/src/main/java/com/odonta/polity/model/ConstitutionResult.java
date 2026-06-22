package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ConstitutionResult(
    UUID id,
    int version,
    ConstitutionStatus status,
    OffsetDateTime ratifiedAt,
    List<ConstitutionInstitutionResult> institutions,
    List<ConstitutionProcedureResult> procedures,
    List<OfficeResult> offices,
    List<ConstitutionPowerResult> powers,
    ConstitutionBootstrapResult bootstrap) {}
