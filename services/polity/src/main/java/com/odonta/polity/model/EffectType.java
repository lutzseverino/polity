package com.odonta.polity.model;

public enum EffectType {
  ADOPT_RESOLUTION,
  ELECT_OFFICE,
  APPLY_SANCTION,
  GRANT_APPEAL,
  VACATE_OFFICE_TERM,
  VOID_OFFICIAL_ACT,
  AMEND_CONSTITUTION,
  DISBAND_POLITY;

  public boolean supportsInstitutionKind(InstitutionKind kind) {
    return kind == defaultInstitutionKind()
        || (this == ADOPT_RESOLUTION && kind == InstitutionKind.COUNCIL);
  }

  private InstitutionKind defaultInstitutionKind() {
    return switch (this) {
      case GRANT_APPEAL, VACATE_OFFICE_TERM, VOID_OFFICIAL_ACT -> InstitutionKind.JUDICIARY;
      case ADOPT_RESOLUTION, ELECT_OFFICE, APPLY_SANCTION, AMEND_CONSTITUTION, DISBAND_POLITY ->
          InstitutionKind.ASSEMBLY;
    };
  }
}
