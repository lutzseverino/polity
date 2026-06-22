package com.odonta.polity.model;

import java.util.UUID;

public record OfficeElectionCandidateTallyResult(UUID membershipId, String name, int ballots) {}
