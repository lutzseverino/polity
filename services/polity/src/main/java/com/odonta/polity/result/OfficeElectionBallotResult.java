package com.odonta.polity.result;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OfficeElectionBallotResult(OffsetDateTime castAt, List<UUID> candidateMembershipIds) {
  public OfficeElectionBallotResult {
    candidateMembershipIds = List.copyOf(candidateMembershipIds);
  }
}
