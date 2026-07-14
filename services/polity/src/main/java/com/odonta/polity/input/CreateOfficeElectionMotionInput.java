package com.odonta.polity.input;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateOfficeElectionMotionInput(
    @NotNull UUID officeId, @NotEmpty @Size(max = 12) List<@NotNull UUID> candidateMembershipIds) {
  public CreateOfficeElectionMotionInput {
    candidateMembershipIds = List.copyOf(candidateMembershipIds);
  }
}
