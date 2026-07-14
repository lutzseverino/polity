package com.odonta.polity.model;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ConstitutionAmendmentState(
    Map<UUID, InstitutionKind> institutionKinds,
    Set<String> officeCodes,
    Map<String, ConstitutionAmendmentProcedureState> procedures,
    Map<PowerCode, ConstitutionAmendmentPowerState> powers,
    Set<UUID> jurisdictionIds,
    Map<String, Long> activeOfficeTermCounts) {
  public ConstitutionAmendmentState {
    institutionKinds = Map.copyOf(institutionKinds);
    officeCodes = Set.copyOf(officeCodes);
    procedures = Map.copyOf(procedures);
    powers = Map.copyOf(powers);
    jurisdictionIds = Set.copyOf(jurisdictionIds);
    activeOfficeTermCounts = Map.copyOf(activeOfficeTermCounts);
  }
}
