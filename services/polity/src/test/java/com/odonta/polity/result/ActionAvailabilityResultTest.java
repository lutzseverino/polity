package com.odonta.polity.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ActionAvailabilityResultTest {
  @Test
  void exposesOnlyAllowedWithoutAReasonOrBlockedWithAReason() {
    ActionAvailabilityResult allowed = ActionAvailabilityResult.allowed();
    ActionAvailabilityResult blocked =
        ActionAvailabilityResult.blocked(ActionUnavailableReason.VOTING_CLOSED);

    assertThat(allowed).isInstanceOf(ActionAvailabilityResult.Allowed.class);
    assertThat(allowed.available()).isTrue();
    assertThat(allowed.reason()).isNull();
    assertThat(blocked).isInstanceOf(ActionAvailabilityResult.Blocked.class);
    assertThat(blocked.available()).isFalse();
    assertThat(blocked.reason()).isEqualTo(ActionUnavailableReason.VOTING_CLOSED);
    assertThatNullPointerException().isThrownBy(() -> ActionAvailabilityResult.blocked(null));
  }

  @Test
  void mapsEveryReasonToAndFromItsStableWireValue() {
    assertThat(Arrays.stream(ActionUnavailableReason.values()))
        .allSatisfy(
            reason ->
                assertThat(ActionUnavailableReason.fromWireValue(reason.wireValue()))
                    .isEqualTo(reason));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ActionUnavailableReason.fromWireValue("new_unregistered_reason"));
  }
}
