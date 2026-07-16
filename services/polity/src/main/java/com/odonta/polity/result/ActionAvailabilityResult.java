package com.odonta.polity.result;

import java.util.Objects;

public sealed interface ActionAvailabilityResult
    permits ActionAvailabilityResult.Allowed, ActionAvailabilityResult.Blocked {
  boolean available();

  ActionUnavailableReason reason();

  public static ActionAvailabilityResult allowed() {
    return new Allowed();
  }

  public static ActionAvailabilityResult blocked(ActionUnavailableReason reason) {
    return new Blocked(reason);
  }

  record Allowed() implements ActionAvailabilityResult {
    @Override
    public boolean available() {
      return true;
    }

    @Override
    public ActionUnavailableReason reason() {
      return null;
    }
  }

  record Blocked(ActionUnavailableReason reason) implements ActionAvailabilityResult {
    public Blocked {
      Objects.requireNonNull(reason, "reason");
    }

    @Override
    public boolean available() {
      return false;
    }
  }
}
