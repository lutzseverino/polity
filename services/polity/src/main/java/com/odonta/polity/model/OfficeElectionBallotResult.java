package com.odonta.polity.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OfficeElectionBallotResult(
    OffsetDateTime castAt, List<UUID> candidateMembershipIds) {}
