package com.odonta.polity.result;

import com.odonta.polity.model.ConstitutionalHealthDiagnostic;
import com.odonta.polity.model.ConstitutionalHealthStatus;
import java.util.List;

public record ConstitutionalHealthResult(
    ConstitutionalHealthStatus status, List<ConstitutionalHealthDiagnostic> diagnostics) {
  public ConstitutionalHealthResult {
    diagnostics = List.copyOf(diagnostics);
  }
}
