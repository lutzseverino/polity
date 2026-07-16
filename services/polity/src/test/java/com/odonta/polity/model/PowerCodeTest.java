package com.odonta.polity.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import org.junit.jupiter.api.Test;

class PowerCodeTest {
  @Test
  void ownsTheDefaultLocalizedNameForEveryPower() {
    assertThat(PowerCode.values())
        .extracting(PowerCode::name, PowerCode::defaultNameKey, PowerCode::defaultStoredName)
        .containsExactly(
            tuple(
                "ADMIT_MEMBER",
                "constitutional_power.admit_member.name",
                "constitutional_power.admit_member.name"),
            tuple(
                "INTRODUCE_MOTION",
                "constitutional_power.introduce_motion.name",
                "constitutional_power.introduce_motion.name"),
            tuple(
                "INTRODUCE_OFFICE_ELECTION",
                "constitutional_power.introduce_office_election.name",
                "constitutional_power.introduce_office_election.name"),
            tuple(
                "INTRODUCE_SANCTION",
                "constitutional_power.introduce_sanction.name",
                "constitutional_power.introduce_sanction.name"),
            tuple(
                "INTRODUCE_APPEAL",
                "constitutional_power.introduce_appeal.name",
                "constitutional_power.introduce_appeal.name"),
            tuple(
                "INTRODUCE_OFFICE_TERM_REVIEW",
                "constitutional_power.introduce_office_term_review.name",
                "constitutional_power.introduce_office_term_review.name"),
            tuple(
                "INTRODUCE_CONSTITUTIONAL_REVIEW",
                "constitutional_power.introduce_constitutional_review.name",
                "constitutional_power.introduce_constitutional_review.name"),
            tuple(
                "INTRODUCE_AMENDMENT",
                "constitutional_power.introduce_amendment.name",
                "constitutional_power.introduce_amendment.name"),
            tuple(
                "INTRODUCE_DISBANDMENT",
                "constitutional_power.introduce_disbandment.name",
                "constitutional_power.introduce_disbandment.name"),
            tuple(
                "REQUEST_CERTIFICATION",
                "constitutional_power.request_certification.name",
                "constitutional_power.request_certification.name"));
  }
}
