package com.odonta.polity.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class OfficialRecordTypeTest {
  @Test
  void constitutionalReviewVoidRemediesAreLimitedToReversibleEffects() {
    assertThat(
            EnumSet.allOf(OfficialRecordType.class).stream()
                .filter(OfficialRecordType::isVoidableByConstitutionalReview))
        .containsExactlyInAnyOrder(
            OfficialRecordType.RESOLUTION_ADOPTED,
            OfficialRecordType.SANCTION_APPLIED,
            OfficialRecordType.OFFICE_ELECTED);
  }
}
