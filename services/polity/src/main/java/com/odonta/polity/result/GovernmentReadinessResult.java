package com.odonta.polity.result;

import com.odonta.polity.model.GovernmentReadinessDiagnostic;
import com.odonta.polity.model.GovernmentReadinessStatus;
import java.util.List;

public record GovernmentReadinessResult(
    GovernmentReadinessStatus status, List<GovernmentReadinessDiagnostic> diagnostics) {
  public GovernmentReadinessResult {
    diagnostics = List.copyOf(diagnostics);
  }
}
