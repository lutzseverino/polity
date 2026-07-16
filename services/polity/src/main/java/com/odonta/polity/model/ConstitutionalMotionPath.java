package com.odonta.polity.model;

public enum ConstitutionalMotionPath {
  ORDINARY_GOVERNANCE(PowerCode.INTRODUCE_MOTION, Procedure.ORDINARY_RESOLUTION),
  OFFICE_ELECTION(PowerCode.INTRODUCE_OFFICE_ELECTION, Procedure.OFFICE_ELECTION),
  SANCTION(PowerCode.INTRODUCE_SANCTION, Procedure.SANCTION),
  APPEAL(PowerCode.INTRODUCE_APPEAL, Procedure.APPEAL),
  OFFICE_TERM_REVIEW(PowerCode.INTRODUCE_OFFICE_TERM_REVIEW, Procedure.OFFICE_TERM_REVIEW),
  CONSTITUTIONAL_REVIEW(PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW, Procedure.CONSTITUTIONAL_REVIEW),
  CONSTITUTION_AMENDMENT(PowerCode.INTRODUCE_AMENDMENT, Procedure.CONSTITUTION_AMENDMENT),
  DISBANDMENT(PowerCode.INTRODUCE_DISBANDMENT, Procedure.DISBANDMENT);

  private final PowerCode introducingPower;
  private final String procedureCode;

  ConstitutionalMotionPath(PowerCode introducingPower, String procedureCode) {
    this.introducingPower = introducingPower;
    this.procedureCode = procedureCode;
  }

  public PowerCode introducingPower() {
    return introducingPower;
  }

  public String procedureCode() {
    return procedureCode;
  }
}
