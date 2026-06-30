package com.odonta.polity.model;

import java.util.List;

public record ConstitutionalHealthResult(
    ConstitutionalHealthStatus status, List<ConstitutionalHealthDiagnostic> diagnostics) {}
