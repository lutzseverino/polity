package com.odonta.polity.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VoteChoiceTest {
  @Test
  void usesTheOpenApiWireValues() {
    assertThat(VoteChoice.YES.wireValue()).isEqualTo("yes");
    assertThat(VoteChoice.NO.wireValue()).isEqualTo("no");
    assertThat(VoteChoice.ABSTAIN.wireValue()).isEqualTo("abstain");
    assertThat(VoteChoice.fromWireValue("yes")).isEqualTo(VoteChoice.YES);
  }

  @Test
  void rejectsUnknownWireValues() {
    assertThatThrownBy(() -> VoteChoice.fromWireValue("maybe"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown vote choice: maybe");
  }
}
