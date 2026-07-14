package com.odonta.polity.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface OfficeElectionBallotProjection {
  UUID getMotionId();

  UUID getMembershipId();

  OffsetDateTime getCastAt();
}
