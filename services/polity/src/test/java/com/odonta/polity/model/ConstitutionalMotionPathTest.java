package com.odonta.polity.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

class ConstitutionalMotionPathTest {
  @Test
  void definesTheConstitutionalPowerAndProcedureForEveryMotionKind() {
    assertThat(ConstitutionalMotionPath.values())
        .extracting(
            ConstitutionalMotionPath::introducingPower, ConstitutionalMotionPath::procedureCode)
        .containsExactly(
            tuple(PowerCode.INTRODUCE_MOTION, Procedure.ORDINARY_RESOLUTION),
            tuple(PowerCode.INTRODUCE_OFFICE_ELECTION, Procedure.OFFICE_ELECTION),
            tuple(PowerCode.INTRODUCE_SANCTION, Procedure.SANCTION),
            tuple(PowerCode.INTRODUCE_APPEAL, Procedure.APPEAL),
            tuple(PowerCode.INTRODUCE_OFFICE_TERM_REVIEW, Procedure.OFFICE_TERM_REVIEW),
            tuple(PowerCode.INTRODUCE_CONSTITUTIONAL_REVIEW, Procedure.CONSTITUTIONAL_REVIEW),
            tuple(PowerCode.INTRODUCE_AMENDMENT, Procedure.CONSTITUTION_AMENDMENT),
            tuple(PowerCode.INTRODUCE_DISBANDMENT, Procedure.DISBANDMENT));
  }
}
