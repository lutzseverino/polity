package com.odonta.polity.model;

import java.util.List;

public record GovernmentReadinessResult(
    GovernmentReadinessStatus status, List<GovernmentReadinessDiagnostic> diagnostics) {}
