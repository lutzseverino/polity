package com.odonta.polity.result;

import java.util.List;

public record GovernmentStructureResult(
    ConstitutionResult constitution,
    List<JurisdictionResult> jurisdictions,
    GovernmentFormationResult formation) {
  public GovernmentStructureResult {
    jurisdictions = List.copyOf(jurisdictions);
  }
}
