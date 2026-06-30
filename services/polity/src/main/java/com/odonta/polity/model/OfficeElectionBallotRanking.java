package com.odonta.polity.model;

import java.util.List;
import java.util.UUID;

public record OfficeElectionBallotRanking(UUID membershipId, List<UUID> candidateMembershipIds) {}
