package com.odonta.polity.model;

public record ActionAvailabilityResult(boolean available, String reason) {
  public static ActionAvailabilityResult allowed() {
    return new ActionAvailabilityResult(true, null);
  }

  public static ActionAvailabilityResult blocked(String reason) {
    return new ActionAvailabilityResult(false, reason);
  }
}
