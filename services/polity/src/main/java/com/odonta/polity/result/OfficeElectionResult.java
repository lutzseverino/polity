package com.odonta.polity.result;

import com.odonta.polity.model.OfficeElectionMethod;
import java.util.List;
import java.util.UUID;

public record OfficeElectionResult(
    UUID officeId,
    String officeCode,
    String officeName,
    String officeNameKey,
    int seatsAvailable,
    OfficeElectionMethod method,
    OfficeElectionBallotResult currentBallot,
    List<OfficeElectionCandidateResult> candidates) {
  public OfficeElectionResult {
    candidates = List.copyOf(candidates);
  }
}
